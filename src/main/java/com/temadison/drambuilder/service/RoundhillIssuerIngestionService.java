package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataHoldingRequest;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class RoundhillIssuerIngestionService {

    private static final String SOURCE_NAV = "roundhill-dailynav";
    private static final String SOURCE_HOLDINGS = "roundhill-holdings";
    private static final String SOURCE_FX = "roundhill-holdings-implied-fx";
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final RestClient restClient;
    private final MarketDataIngestionService marketDataIngestionService;
    private final String baseUrl;
    private final Clock clock;

    @Autowired
    public RoundhillIssuerIngestionService(
            RestClient.Builder restClientBuilder,
            MarketDataIngestionService marketDataIngestionService,
            @Value("${app.issuer.roundhill.base-url:https://www.roundhillinvestments.com/assets/data}") String baseUrl
    ) {
        this(restClientBuilder, marketDataIngestionService, baseUrl, Clock.system(ZoneId.of("America/New_York")));
    }

    RoundhillIssuerIngestionService(
            RestClient.Builder restClientBuilder,
            MarketDataIngestionService marketDataIngestionService,
            String baseUrl,
            Clock clock
    ) {
        this.restClient = restClientBuilder.build();
        this.marketDataIngestionService = marketDataIngestionService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.clock = clock;
    }

    public void ingestLatest(String window) {
        String source = "roundhill-" + window;
        MarketDataIngestionRequest request;
        try {
            request = latestIngestionRequest();
        } catch (Exception exception) {
            marketDataIngestionService.recordFailure(source, "roundhill-public-csv", exception);
            throw exception;
        }
        marketDataIngestionService.ingest(source, "roundhill-public-csv", request);
    }

    MarketDataIngestionRequest latestIngestionRequest() {
        Map<LocalDate, Optional<HoldingsExport>> holdingsCache = new LinkedHashMap<>();
        NavRow nav = latestDramNav();
        HoldingsExport navHoldings = holdingsForFileDate(nav.rateDate(), holdingsCache)
                .orElseThrow(() -> new IllegalStateException("No Roundhill DRAM holdings CSV was found for NAV date " + nav.rateDate()));
        HoldingsExport latestHoldings = latestHoldings(holdingsCache)
                .orElse(navHoldings);
        HoldingsExport latestPrior = priorHoldingsBefore(latestHoldings.rowDate(), holdingsCache);

        List<PriceSnapshotRequest> prices = new ArrayList<>();
        prices.add(new PriceSnapshotRequest(
                "DRAM",
                "Roundhill Memory ETF",
                "BZX",
                "USD",
                nav.marketPrice(),
                SOURCE_NAV,
                observedAt(nav.rateDate())
        ));
        prices.addAll(holdingPrices(latestPrior));
        prices.addAll(holdingPrices(latestHoldings));
        if (!navHoldings.rowDate().equals(latestHoldings.rowDate())) {
            prices.addAll(holdingPrices(priorHoldingsBefore(navHoldings.rowDate(), holdingsCache)));
            prices.addAll(holdingPrices(navHoldings));
        }

        List<FxRateSnapshotRequest> fxRates = new ArrayList<>();
        fxRates.addAll(impliedFxRates(latestPrior));
        fxRates.addAll(impliedFxRates(latestHoldings));
        if (!navHoldings.rowDate().equals(latestHoldings.rowDate())) {
            fxRates.addAll(impliedFxRates(priorHoldingsBefore(navHoldings.rowDate(), holdingsCache)));
            fxRates.addAll(impliedFxRates(navHoldings));
        }

        List<OfficialNavSnapshotRequest> officialNavs = List.of(new OfficialNavSnapshotRequest(
                "DRAM",
                "Roundhill Memory ETF",
                nav.nav(),
                "USD",
                SOURCE_NAV,
                nav.rateDate(),
                observedAt(nav.rateDate())
        ));

        MarketDataSnapshotRequest snapshot = new MarketDataSnapshotRequest(
                nav.rateDate(),
                null,
                nav.marketPrice(),
                "DRAM",
                "BZX",
                navHoldings.snapshotHoldings()
        );

        return new MarketDataIngestionRequest(prices, fxRates, officialNavs, snapshot);
    }

    private NavRow latestDramNav() {
        String csv = fetch(baseUrl + "/FilepointRoundhill.40RU.RU_DailyNAV.csv");
        return parseRows(csv).stream()
                .filter(row -> "DRAM".equalsIgnoreCase(row.get("Fund Ticker")))
                .findFirst()
                .map(row -> new NavRow(
                        decimal(row.get("NAV")),
                        decimal(row.get("Market Price")),
                        parseDate(row.get("Rate Date"))
                ))
                .orElseThrow(() -> new IllegalStateException("Roundhill Daily NAV CSV did not include DRAM"));
    }

    private HoldingsExport priorHoldingsBefore(LocalDate currentRowDate, Map<LocalDate, Optional<HoldingsExport>> holdingsCache) {
        for (int offset = 1; offset < 30; offset++) {
            Optional<HoldingsExport> export = holdingsForFileDate(currentRowDate.minusDays(offset), holdingsCache)
                    .filter(holdings -> holdings.rowDate().isBefore(currentRowDate));
            if (export.isPresent()) {
                return export.get();
            }
        }
        throw new IllegalStateException("No prior Roundhill DRAM holdings CSV was found before " + currentRowDate);
    }

    private Optional<HoldingsExport> latestHoldings(Map<LocalDate, Optional<HoldingsExport>> holdingsCache) {
        LocalDate today = LocalDate.now(clock);
        for (int offset = 0; offset < 30; offset++) {
            Optional<HoldingsExport> export = holdingsForFileDate(today.minusDays(offset), holdingsCache)
                    .filter(holdings -> !holdings.rowDate().isAfter(today));
            if (export.isPresent()) {
                return export;
            }
        }
        return Optional.empty();
    }

    private Optional<HoldingsExport> holdingsForFileDate(LocalDate fileDate, Map<LocalDate, Optional<HoldingsExport>> holdingsCache) {
        return holdingsCache.computeIfAbsent(fileDate, this::holdingsForFileDate);
    }

    private Optional<HoldingsExport> holdingsForFileDate(LocalDate fileDate) {
        String csv;
        try {
            csv = fetch(baseUrl + "/FilepointRoundhill.40RU.RU_Holdings_" + fileDate.format(FILE_DATE) + ".csv");
        } catch (RestClientResponseException exception) {
            return Optional.empty();
        }
        if (!csv.startsWith("Date,Account,StockTicker")) {
            return Optional.empty();
        }

        List<Map<String, String>> rows = parseRows(csv).stream()
                .filter(row -> "DRAM".equalsIgnoreCase(row.get("Account")))
                .toList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        LocalDate rowDate = parseDate(rows.getFirst().get("Date"));
        return Optional.of(new HoldingsExport(fileDate, rowDate, rows));
    }

    private List<PriceSnapshotRequest> holdingPrices(HoldingsExport export) {
        return HoldingDefinition.DIRECT_ROWS.entrySet().stream()
                .map(entry -> {
                    String sourceTicker = entry.getKey();
                    HoldingDefinition definition = entry.getValue();
                    Map<String, String> row = export.requireRow(sourceTicker);
                    return new PriceSnapshotRequest(
                            definition.ticker(),
                            definition.name(),
                            definition.exchange(),
                            definition.currency(),
                            decimal(row.get("Price")),
                            SOURCE_HOLDINGS,
                            observedAt(export.rowDate())
                    );
                })
                .toList();
    }

    private List<FxRateSnapshotRequest> impliedFxRates(HoldingsExport export) {
        Map<String, List<BigDecimal>> samples = new LinkedHashMap<>();
        HoldingDefinition.DIRECT_ROWS.forEach((sourceTicker, definition) -> {
            if ("USD".equals(definition.currency())) {
                return;
            }
            Map<String, String> row = export.requireRow(sourceTicker);
            BigDecimal shares = decimal(row.get("Shares"));
            BigDecimal price = decimal(row.get("Price"));
            BigDecimal marketValue = decimal(row.get("MarketValue"));
            BigDecimal rate = marketValue.divide(shares.multiply(price), 12, RoundingMode.HALF_UP);
            samples.computeIfAbsent(definition.currency(), ignored -> new ArrayList<>()).add(rate);
        });

        return samples.entrySet().stream()
                .map(entry -> new FxRateSnapshotRequest(
                        entry.getKey(),
                        "USD",
                        average(entry.getValue()),
                        SOURCE_FX,
                        observedAt(export.rowDate())
                ))
                .toList();
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(new BigDecimal(values.size()), 12, RoundingMode.HALF_UP);
    }

    private String fetch(String url) {
        String body = restClient.get().uri(url).retrieve().body(String.class);
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Roundhill returned an empty response for " + url);
        }
        return body.stripLeading();
    }

    private List<Map<String, String>> parseRows(String csv) {
        List<List<String>> rows = parseCsv(csv);
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> headers = rows.getFirst();
        List<Map<String, String>> parsed = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                String value = column < row.size() ? row.get(column) : "";
                values.put(headers.get(column), value);
            }
            parsed.add(values);
        }
        return parsed;
    }

    private List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < csv.length(); i++) {
            char current = csv.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == '"') {
                quoted = true;
            } else if (current == ',') {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if (current == '\n') {
                row.add(field.toString().trim());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
            } else if (current != '\r') {
                field.append(current);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("CSV contains an unterminated quoted field");
        }
        row.add(field.toString().trim());
        if (row.stream().anyMatch(value -> !value.isBlank())) {
            rows.add(row);
        }
        return rows;
    }

    private LocalDate parseDate(String value) {
        return LocalDate.parse(value, CSV_DATE);
    }

    private Instant observedAt(LocalDate date) {
        return date.atTime(20, 0).toInstant(ZoneOffset.UTC);
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value.replace("%", "").replace(",", "").trim());
    }

    private record NavRow(BigDecimal nav, BigDecimal marketPrice, LocalDate rateDate) {
    }

    private record HoldingsExport(LocalDate fileDate, LocalDate rowDate, List<Map<String, String>> rows) {

        private Map<String, String> requireRow(String stockTicker) {
            return rows.stream()
                    .filter(row -> stockTicker.equals(row.get("StockTicker")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Roundhill holdings CSV is missing DRAM row " + stockTicker));
        }

        private List<MarketDataHoldingRequest> snapshotHoldings() {
            Map<String, BigDecimal> weights = new LinkedHashMap<>();
            HoldingDefinition.DIRECT_ROWS.keySet().forEach(ticker -> weights.put(ticker, BigDecimal.ZERO));

            for (Map<String, String> row : rows) {
                String stockTicker = row.get("StockTicker");
                String target = HoldingDefinition.SWAP_WEIGHT_TARGETS.getOrDefault(stockTicker, stockTicker);
                if (weights.containsKey(target)) {
                    weights.put(target, weights.get(target).add(new BigDecimal(row.get("Weightings").replace("%", ""))
                            .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
                }
            }

            return weights.entrySet().stream()
                    .map(entry -> {
                        HoldingDefinition definition = HoldingDefinition.DIRECT_ROWS.get(entry.getKey());
                        return new MarketDataHoldingRequest(
                                definition.ticker(),
                                definition.name(),
                                definition.exchange(),
                                definition.currency(),
                                entry.getValue().setScale(4, RoundingMode.HALF_UP)
                        );
                    })
                    .toList();
        }
    }

    private record HoldingDefinition(String ticker, String name, String exchange, String currency) {

        private static final Map<String, HoldingDefinition> DIRECT_ROWS = directRows();
        private static final Map<String, String> SWAP_WEIGHT_TARGETS = swapWeightTargets();

        private static Map<String, HoldingDefinition> directRows() {
            Map<String, HoldingDefinition> rows = new LinkedHashMap<>();
            rows.put("000660 KS", new HoldingDefinition("000660", "SK hynix", "KRX", "KRW"));
            rows.put("005930 KS", new HoldingDefinition("005930", "Samsung Electronics", "KRX", "KRW"));
            rows.put("2337 TT", new HoldingDefinition("2337", "Macronix International", "TWSE", "TWD"));
            rows.put("2344 TT", new HoldingDefinition("2344", "Winbond Electronics", "TWSE", "TWD"));
            rows.put("2408 TT", new HoldingDefinition("2408", "Nanya Technology", "TWSE", "TWD"));
            rows.put("285A JP", new HoldingDefinition("285A", "Kioxia Holdings", "TSE", "JPY"));
            rows.put("603986 C1", new HoldingDefinition("603986", "GigaDevice Semiconductor", "SSE", "CNY"));
            rows.put("8299 TT", new HoldingDefinition("8299", "Phison Electronics", "TPEX", "TWD"));
            rows.put("MU", new HoldingDefinition("MU", "Micron Technology", "NASDAQ", "USD"));
            rows.put("SNDK", new HoldingDefinition("SNDK", "SanDisk", "NASDAQ", "USD"));
            rows.put("STX", new HoldingDefinition("STX", "Seagate Technology", "NASDAQ", "USD"));
            rows.put("WDC", new HoldingDefinition("WDC", "Western Digital", "NASDAQ", "USD"));
            return rows;
        }

        private static Map<String, String> swapWeightTargets() {
            Map<String, String> targets = new LinkedHashMap<>();
            targets.put("595112103 TRS 050427 NM", "MU");
            targets.put("595112103 TRS 052427 GS", "MU");
            targets.put("6450267 TRS 052427 GS", "000660 KS");
            targets.put("6771720 TRS 052427 GS", "005930 KS");
            return targets;
        }
    }
}

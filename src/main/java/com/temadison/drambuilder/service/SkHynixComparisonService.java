package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.MarketCapitalizationProperties;
import com.temadison.drambuilder.domain.FxRateSnapshot;
import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.SkHynixComparisonPointResponse;
import com.temadison.drambuilder.dto.SkHynixComparisonResponse;
import com.temadison.drambuilder.dto.SkHynixParityResponse;
import com.temadison.drambuilder.repository.FxRateSnapshotRepository;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkHynixComparisonService {

    private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String ADR_TICKER = "SKHY";
    private static final String ADR_EXCHANGE = "NASDAQ";
    private static final String LOCAL_TICKER = "000660";
    private static final String LOCAL_EXCHANGE = "KRX";
    private static final String MICRON_TICKER = "MU";
    private static final String KRW = "KRW";
    private static final String USD = "USD";
    private static final LocalDate MARKET_CAP_START_DATE = LocalDate.of(2026, 7, 1);

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final FxRateSnapshotRepository fxRateSnapshotRepository;
    private final MarketCapitalizationProperties marketCapitalizationProperties;

    public SkHynixComparisonService(
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            MarketCapitalizationProperties marketCapitalizationProperties
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.marketCapitalizationProperties = marketCapitalizationProperties;
    }

    @Transactional(readOnly = true)
    public SkHynixComparisonResponse comparison() {
        List<PriceSnapshot> adrPrices = prices(ADR_TICKER, ADR_EXCHANGE);
        List<PriceSnapshot> localPrices = prices(LOCAL_TICKER, LOCAL_EXCHANGE);
        List<PriceSnapshot> micronPrices = prices(MICRON_TICKER, ADR_EXCHANGE);

        SkHynixParityResponse latestParity = latestParity(adrPrices, localPrices);
        List<SkHynixComparisonPointResponse> performance = List.of(
                        marketCapSeries(
                                "SKHY",
                                "SK hynix ADR",
                                adrPrices,
                                price -> price.getPrice(),
                                skHynixAdrEquivalentShares()
                        ),
                        marketCapSeries(
                                "000660",
                                "SK hynix KRX",
                                localPrices,
                                this::usdAdjustedLocalPrice,
                                marketCapitalizationProperties.getSkHynixLocalSharesOutstanding()
                        ),
                        marketCapSeries(
                                "MU",
                                "Micron",
                                micronPrices,
                                price -> price.getPrice(),
                                marketCapitalizationProperties.getMicronSharesOutstanding()
                        )
                ).stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(SkHynixComparisonPointResponse::date).thenComparing(SkHynixComparisonPointResponse::symbol))
                .toList();

        return new SkHynixComparisonResponse(
                ADR_TICKER,
                LOCAL_TICKER,
                LOCAL_EXCHANGE,
                marketCapitalizationProperties.getSkHynixAdrPerLocalShare(),
                marketCapitalizationProperties.getMicronSharesOutstanding(),
                marketCapitalizationProperties.getSkHynixLocalSharesOutstanding(),
                latestParity,
                performance
        );
    }

    private List<PriceSnapshot> prices(String ticker, String exchange) {
        return priceSnapshotRepository.findTop120BySecurityTickerAndSecurityExchangeOrderByObservedAtAsc(
                ticker.toUpperCase(Locale.ROOT),
                exchange.toUpperCase(Locale.ROOT)
        );
    }

    private SkHynixParityResponse latestParity(List<PriceSnapshot> adrPrices, List<PriceSnapshot> localPrices) {
        if (adrPrices.isEmpty() || localPrices.isEmpty()) {
            return null;
        }

        Map<LocalDate, PriceSnapshot> adrByDate = latestByDate(adrPrices);
        Map<LocalDate, PriceSnapshot> localByDate = latestByDate(localPrices);
        Optional<LocalDate> date = adrByDate.keySet().stream()
                .filter(localByDate::containsKey)
                .filter(this::hasFxForDate)
                .max(LocalDate::compareTo);
        if (date.isEmpty()) {
            return null;
        }

        PriceSnapshot adr = adrByDate.get(date.get());
        PriceSnapshot local = localByDate.get(date.get());
        FxRateSnapshot fx = fxForDate(date.get())
                .orElseThrow(() -> new IllegalStateException("No FX rate snapshot exists for KRW/USD as of " + date.get()));
        BigDecimal localEquivalent = local.getPrice()
                .multiply(fx.getRate(), MATH_CONTEXT)
                .divide(new BigDecimal(marketCapitalizationProperties.getSkHynixAdrPerLocalShare()), MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal premiumDiscount = adr.getPrice()
                .divide(localEquivalent, MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT)
                .multiply(ONE_HUNDRED, MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);

        return new SkHynixParityResponse(
                date.get(),
                adr.getPrice(),
                local.getPrice(),
                fx.getRate(),
                localEquivalent,
                premiumDiscount,
                adr.getObservedAt(),
                local.getObservedAt(),
                fx.getObservedAt()
        );
    }

    private List<SkHynixComparisonPointResponse> marketCapSeries(
            String symbol,
            String label,
            List<PriceSnapshot> prices,
            Function<PriceSnapshot, BigDecimal> priceMapper,
            BigDecimal sharesOutstanding
    ) {
        List<DailyValue> values = latestByDate(prices).entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(MARKET_CAP_START_DATE))
                .map(entry -> toDailyMarketCap(entry.getKey(), entry.getValue(), priceMapper, sharesOutstanding))
                .filter(value -> value.price() != null && value.price().signum() > 0 && value.marketCapUsd().signum() > 0)
                .sorted(Comparator.comparing(DailyValue::date))
                .toList();
        if (values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(value -> new SkHynixComparisonPointResponse(
                        value.date(),
                        symbol,
                        label,
                        value.price().setScale(6, RoundingMode.HALF_UP),
                        sharesOutstanding.setScale(6, RoundingMode.HALF_UP),
                        value.marketCapUsd().setScale(6, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private DailyValue toDailyMarketCap(
            LocalDate date,
            PriceSnapshot price,
            Function<PriceSnapshot, BigDecimal> priceMapper,
            BigDecimal sharesOutstanding
    ) {
        BigDecimal usdPrice = priceMapper.apply(price);
        if (usdPrice == null || sharesOutstanding == null) {
            return new DailyValue(date, null, BigDecimal.ZERO);
        }
        return new DailyValue(date, usdPrice, usdPrice.multiply(sharesOutstanding, MATH_CONTEXT));
    }

    private BigDecimal skHynixAdrEquivalentShares() {
        return marketCapitalizationProperties.getSkHynixLocalSharesOutstanding()
                .multiply(new BigDecimal(marketCapitalizationProperties.getSkHynixAdrPerLocalShare()), MATH_CONTEXT);
    }

    private BigDecimal usdAdjustedLocalPrice(PriceSnapshot price) {
        return fxForDate(toDate(price.getObservedAt()))
                .map(fx -> price.getPrice().multiply(fx.getRate(), MATH_CONTEXT))
                .orElse(null);
    }

    private Map<LocalDate, PriceSnapshot> latestByDate(List<PriceSnapshot> prices) {
        return prices.stream()
                .collect(Collectors.toMap(
                        price -> toDate(price.getObservedAt()),
                        Function.identity(),
                        (left, right) -> left.getObservedAt().isAfter(right.getObservedAt()) ? left : right
                ));
    }

    private boolean hasFxForDate(LocalDate date) {
        return fxForDate(date).isPresent();
    }

    private Optional<FxRateSnapshot> fxForDate(LocalDate date) {
        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return fxRateSnapshotRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
                        KRW,
                        USD,
                        start,
                        end
                );
    }

    private LocalDate toDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private record DailyValue(LocalDate date, BigDecimal price, BigDecimal marketCapUsd) {
    }
}

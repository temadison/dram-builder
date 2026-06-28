package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.BulkMarketDataImportResponse;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataCsvImportResponse;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotResponse;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MarketDataCsvImportService {

    private final MarketDataService marketDataService;

    public MarketDataCsvImportService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public MarketDataCsvImportResponse importCsv(String csv) {
        List<List<String>> rows = parse(csv);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV import requires a header row");
        }

        Map<String, Integer> headers = headers(rows.getFirst());
        List<PriceSnapshotRequest> prices = new ArrayList<>();
        List<FxRateSnapshotRequest> fxRates = new ArrayList<>();
        List<OfficialNavSnapshotRequest> officialNavs = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }

            int lineNumber = i + 1;
            String type = required(row, headers, "type", lineNumber).toLowerCase(Locale.ROOT);
            if ("price".equals(type)) {
                prices.add(new PriceSnapshotRequest(
                        required(row, headers, "ticker", lineNumber),
                        required(row, headers, "name", lineNumber),
                        required(row, headers, "exchange", lineNumber),
                        required(row, headers, "currency", lineNumber),
                        decimal(required(row, headers, "price", lineNumber), "price", lineNumber),
                        required(row, headers, "source", lineNumber),
                        optionalInstant(value(row, headers, "observedat"), lineNumber)
                ));
            } else if ("fx".equals(type) || "fx_rate".equals(type)) {
                fxRates.add(new FxRateSnapshotRequest(
                        required(row, headers, "basecurrency", lineNumber),
                        required(row, headers, "quotecurrency", lineNumber),
                        decimal(required(row, headers, "rate", lineNumber), "rate", lineNumber),
                        required(row, headers, "source", lineNumber),
                        optionalInstant(value(row, headers, "observedat"), lineNumber)
                ));
            } else if ("official_nav".equals(type) || "officialnav".equals(type) || "nav".equals(type)) {
                officialNavs.add(new OfficialNavSnapshotRequest(
                        required(row, headers, "ticker", lineNumber),
                        required(row, headers, "name", lineNumber),
                        decimal(required(row, headers, "nav", lineNumber), "nav", lineNumber),
                        required(row, headers, "currency", lineNumber),
                        required(row, headers, "source", lineNumber),
                        date(required(row, headers, "asofdate", lineNumber), "asOfDate", lineNumber),
                        optionalInstant(value(row, headers, "observedat"), lineNumber)
                ));
            } else {
                throw new IllegalArgumentException("Unsupported market data CSV type on line " + lineNumber + ": " + type);
            }
        }

        if (prices.isEmpty() && fxRates.isEmpty() && officialNavs.isEmpty()) {
            throw new IllegalArgumentException("At least one price, FX rate, or official NAV snapshot is required");
        }

        BulkMarketDataImportResponse importedMarketData = prices.isEmpty() && fxRates.isEmpty()
                ? new BulkMarketDataImportResponse(0, 0, List.of(), List.of())
                : marketDataService.importMarketData(new BulkMarketDataImportRequest(prices, fxRates));
        List<OfficialNavSnapshotResponse> importedOfficialNavs = officialNavs.stream()
                .map(marketDataService::createOfficialNavSnapshot)
                .toList();

        return new MarketDataCsvImportResponse(
                importedMarketData.pricesImported(),
                importedMarketData.fxRatesImported(),
                importedOfficialNavs.size(),
                importedMarketData.prices(),
                importedMarketData.fxRates(),
                importedOfficialNavs
        );
    }

    private List<List<String>> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

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
        if (!isBlankRow(row)) {
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Integer> headers(List<String> headerRow) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String header = normalizeHeader(headerRow.get(i));
            if (!header.isBlank()) {
                headers.put(header, i);
            }
        }
        if (!headers.containsKey("type")) {
            throw new IllegalArgumentException("CSV import requires a type column");
        }
        return headers;
    }

    private String required(List<String> row, Map<String, Integer> headers, String column, int lineNumber) {
        String value = value(row, headers, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing " + column + " on line " + lineNumber);
        }
        return value;
    }

    private String value(List<String> row, Map<String, Integer> headers, String column) {
        Integer index = headers.get(column);
        if (index == null || index >= row.size()) {
            return "";
        }
        return row.get(index).trim();
    }

    private BigDecimal decimal(String value, String column, int lineNumber) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() <= 0) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + column + " on line " + lineNumber + ": " + value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + column + " on line " + lineNumber + ": " + value);
        }
    }

    private Instant optionalInstant(String value, int lineNumber) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid observedAt on line " + lineNumber + ": " + value);
        }
    }

    private LocalDate date(String value, String column, int lineNumber) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + column + " on line " + lineNumber + ": " + value);
        }
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(String::isBlank);
    }

    private String normalizeHeader(String header) {
        return header.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }
}

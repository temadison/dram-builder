package com.temadison.drambuilder.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.temadison.drambuilder.config.TwelveDataProviderProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TwelveDataClient {

    private final RestClient restClient;
    private final TwelveDataProviderProperties properties;

    public TwelveDataClient(RestClient.Builder restClientBuilder, TwelveDataProviderProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.properties = properties;
    }

    public List<DailyClose> dailyCloses(String symbol, String exchange, int outputSize) {
        TwelveDataTimeSeriesResponse response = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "1day")
                            .queryParam("outputsize", outputSize)
                            .queryParam("apikey", properties.getApiKey());
                    if (exchange != null && !exchange.isBlank()) {
                        builder.queryParam("exchange", exchange);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(TwelveDataTimeSeriesResponse.class);

        if (response == null) {
            throw new IllegalStateException("Twelve Data returned an empty response for " + symbol);
        }
        if ("error".equalsIgnoreCase(response.status())) {
            throw new IllegalStateException("Twelve Data error for " + symbol + ": " + response.message());
        }
        if (response.values() == null || response.values().isEmpty()) {
            throw new IllegalStateException("Twelve Data returned no daily closes for " + symbol);
        }

        return response.values().stream()
                .map(value -> new DailyClose(observedAt(value.datetime()), value.close()))
                .sorted(Comparator.comparing(DailyClose::observedAt).reversed())
                .toList();
    }

    private Instant observedAt(String datetime) {
        return LocalDate.parse(datetime).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public record DailyClose(Instant observedAt, BigDecimal close) {
    }

    private record TwelveDataTimeSeriesResponse(
            String status,
            String message,
            List<TwelveDataValue> values
    ) {
    }

    private record TwelveDataValue(
            String datetime,
            @JsonProperty("close") BigDecimal close
    ) {
    }
}

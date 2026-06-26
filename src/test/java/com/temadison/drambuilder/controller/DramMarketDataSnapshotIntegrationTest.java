package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataHoldingRequest;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DramMarketDataSnapshotIntegrationTest {

    private static final Instant PRIOR_OBSERVED_AT = Instant.parse("2026-06-25T20:00:00Z");
    private static final Instant CURRENT_OBSERVED_AT = Instant.parse("2026-06-26T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsDramSnapshotFromStoredMarketData() throws Exception {
        createPrice("DRAM", "Roundhill Memory ETF", "NYSEARCA", "USD", "81.50", CURRENT_OBSERVED_AT);
        createPrice("000660", "SK hynix", "KRX", "KRW", "110000.00", PRIOR_OBSERVED_AT);
        createPrice("000660", "SK hynix", "KRX", "KRW", "114000.00", CURRENT_OBSERVED_AT);
        createPrice("MU", "Micron Technology", "NASDAQ", "USD", "105.00", PRIOR_OBSERVED_AT);
        createPrice("MU", "Micron Technology", "NASDAQ", "USD", "108.00", CURRENT_OBSERVED_AT);
        createPrice("005930", "Samsung Electronics", "KRX", "KRW", "77600.00", PRIOR_OBSERVED_AT);
        createPrice("005930", "Samsung Electronics", "KRX", "KRW", "79000.00", CURRENT_OBSERVED_AT);
        createFxRate("KRW", "USD", "0.00080000", PRIOR_OBSERVED_AT);
        createFxRate("KRW", "USD", "0.00081000", CURRENT_OBSERVED_AT);

        MarketDataSnapshotRequest request = new MarketDataSnapshotRequest(
                LocalDate.of(2026, 6, 26),
                null,
                new BigDecimal("76.31"),
                null,
                null,
                List.of(
                        new MarketDataHoldingRequest("000660", "SK hynix", "KRX", "KRW", new BigDecimal("0.26")),
                        new MarketDataHoldingRequest("MU", "Micron Technology", "NASDAQ", "USD", new BigDecimal("0.19")),
                        new MarketDataHoldingRequest("005930", "Samsung Electronics", "KRX", "KRW", new BigDecimal("0.15"))
                )
        );

        mockMvc.perform(post("/api/dram/snapshot/from-market-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId", notNullValue()))
                .andExpect(jsonPath("$.etfTicker", is("DRAM")))
                .andExpect(jsonPath("$.marketPrice", comparesEqualTo(81.50)))
                .andExpect(jsonPath("$.purchasePrice", comparesEqualTo(76.31)))
                .andExpect(jsonPath("$.holdings", hasSize(3)))
                .andExpect(jsonPath("$.holdings[0].ticker", is("000660")))
                .andExpect(jsonPath("$.holdings[0].currentPrice", comparesEqualTo(114000.00)))
                .andExpect(jsonPath("$.holdings[0].priorPrice", comparesEqualTo(110000.00)))
                .andExpect(jsonPath("$.holdings[0].currentFxToUsd", comparesEqualTo(0.00081000)))
                .andExpect(jsonPath("$.holdings[0].priorFxToUsd", comparesEqualTo(0.00080000)))
                .andExpect(jsonPath("$.attribution.hasPriorSnapshot", is(false)));
    }

    @Test
    void fromMarketDataReturnsNotFoundWhenHoldingPriceIsMissing() throws Exception {
        createPrice("DRAM", "Roundhill Memory ETF", "NYSEARCA", "USD", "81.50", CURRENT_OBSERVED_AT);

        MarketDataSnapshotRequest request = new MarketDataSnapshotRequest(
                LocalDate.of(2026, 6, 26),
                null,
                new BigDecimal("76.31"),
                null,
                null,
                List.of(new MarketDataHoldingRequest(
                        "MU",
                        "Micron Technology",
                        "NASDAQ",
                        "USD",
                        new BigDecimal("0.19")
                ))
        );

        mockMvc.perform(post("/api/dram/snapshot/from-market-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")))
                .andExpect(jsonPath("$.message", is("No price snapshot exists for MU on NASDAQ")));
    }

    private void createPrice(String ticker, String name, String exchange, String currency, String price, Instant observedAt)
            throws Exception {
        PriceSnapshotRequest request = new PriceSnapshotRequest(
                ticker,
                name,
                exchange,
                currency,
                new BigDecimal(price),
                "manual-test",
                observedAt
        );
        mockMvc.perform(post("/api/market-data/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createFxRate(String baseCurrency, String quoteCurrency, String rate, Instant observedAt) throws Exception {
        FxRateSnapshotRequest request = new FxRateSnapshotRequest(
                baseCurrency,
                quoteCurrency,
                new BigDecimal(rate),
                "manual-test",
                observedAt
        );
        mockMvc.perform(post("/api/market-data/fx-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}

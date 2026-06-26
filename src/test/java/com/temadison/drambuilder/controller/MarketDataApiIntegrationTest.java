package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.time.Instant;
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
class MarketDataApiIntegrationTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-06-26T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void storesAndReadsLatestPriceSnapshot() throws Exception {
        PriceSnapshotRequest request = new PriceSnapshotRequest(
                "mu",
                "Micron Technology",
                "nasdaq",
                "usd",
                new BigDecimal("108.25"),
                "manual-test",
                OBSERVED_AT
        );

        mockMvc.perform(post("/api/market-data/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.ticker", is("MU")))
                .andExpect(jsonPath("$.exchange", is("NASDAQ")))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.price", comparesEqualTo(108.25)))
                .andExpect(jsonPath("$.source", is("manual-test")));

        mockMvc.perform(get("/api/market-data/prices/NASDAQ/MU/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("MU")))
                .andExpect(jsonPath("$.price", comparesEqualTo(108.25)));
    }

    @Test
    void storesAndReadsLatestFxRateSnapshotAndSummary() throws Exception {
        FxRateSnapshotRequest request = new FxRateSnapshotRequest(
                "krw",
                "usd",
                new BigDecimal("0.00081000"),
                "manual-test",
                OBSERVED_AT
        );

        mockMvc.perform(post("/api/market-data/fx-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.baseCurrency", is("KRW")))
                .andExpect(jsonPath("$.quoteCurrency", is("USD")))
                .andExpect(jsonPath("$.rate", comparesEqualTo(0.00081000)));

        mockMvc.perform(get("/api/market-data/fx-rates/KRW/USD/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate", comparesEqualTo(0.00081000)));

        mockMvc.perform(get("/api/market-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestPrices", hasSize(0)))
                .andExpect(jsonPath("$.latestFxRates", hasSize(1)));
    }

    @Test
    void latestPriceReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/market-data/prices/NASDAQ/MU/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")));
    }
}

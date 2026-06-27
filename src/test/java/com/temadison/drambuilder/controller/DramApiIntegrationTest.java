package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.fixtures.DramFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DramApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seededSnapshotsSupportLatestScenarioAndBridgeScoreEndpoints() throws Exception {
        createSnapshot(DramFixtures.baselineSnapshot());
        createSnapshot(DramFixtures.followUpSnapshot());

        mockMvc.perform(get("/api/dram/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId", notNullValue()))
                .andExpect(jsonPath("$.etfTicker", is("DRAM")))
                .andExpect(jsonPath("$.holdings", hasSize(4)))
                .andExpect(jsonPath("$.attribution.hasPriorSnapshot", is(true)))
                .andExpect(jsonPath("$.attribution.topContributors", hasSize(4)));

        mockMvc.perform(post("/api/dram/scenario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DramFixtures.upsideScenario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioRunId", notNullValue()))
                .andExpect(jsonPath("$.baselineSnapshotId", notNullValue()))
                .andExpect(jsonPath("$.estimatedMovePercent", comparesEqualTo(3.963)))
                .andExpect(jsonPath("$.projectedMarketPrice", comparesEqualTo(84.729845)))
                .andExpect(jsonPath("$.dollarImpactVsPurchasePrice", comparesEqualTo(8.419845)))
                .andExpect(jsonPath("$.holdings", hasSize(4)));

        mockMvc.perform(get("/api/dram/bridge-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", greaterThan(0.0)))
                .andExpect(jsonPath("$.rotationSignal", notNullValue()))
                .andExpect(jsonPath("$.targetExposureWeight", comparesEqualTo(0.45)));

        mockMvc.perform(post("/api/dram/bridge-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DramFixtures.bridgeScoreOverrides())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.liquidityScore", comparesEqualTo(75.0)))
                .andExpect(jsonPath("$.components.trackingConfidenceScore", comparesEqualTo(70.0)))
                .andExpect(jsonPath("$.components.timingRiskScore", comparesEqualTo(55.0)));
    }

    @Test
    void bridgeScoreReturnsNotFoundWhenNoSnapshotExists() throws Exception {
        mockMvc.perform(get("/api/dram/bridge-score"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")));
    }

    @Test
    void manualSnapshotRejectsTotalHoldingWeightAboveOne() throws Exception {
        SnapshotRequest request = new SnapshotRequest(
                LocalDate.of(2026, 6, 26),
                new BigDecimal("81.50"),
                DramFixtures.PURCHASE_PRICE,
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.70"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.40")
                )
        );

        mockMvc.perform(post("/api/dram/snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("Total holding weight must not exceed 1.0")));
    }

    private void createSnapshot(Object snapshot) throws Exception {
        mockMvc.perform(post("/api/dram/snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snapshot)))
                .andExpect(status().isOk());
    }

    private HoldingInput holding(String ticker, String name, String exchange, String currency, String weight) {
        return new HoldingInput(
                ticker,
                name,
                exchange,
                currency,
                new BigDecimal(weight),
                new BigDecimal("100"),
                new BigDecimal("100"),
                BigDecimal.ONE,
                BigDecimal.ONE
        );
    }
}

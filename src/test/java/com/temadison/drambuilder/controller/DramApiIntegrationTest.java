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
import com.temadison.drambuilder.fixtures.DramFixtures;
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

    private void createSnapshot(Object snapshot) throws Exception {
        mockMvc.perform(post("/api/dram/snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snapshot)))
                .andExpect(status().isOk());
    }
}

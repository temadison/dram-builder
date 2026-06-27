package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.temadison.drambuilder.service.MarketDataProviderIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.provider.twelvedata.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TwelveDataProviderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketDataProviderIngestionService marketDataProviderIngestionService;

    @Test
    void enabledProviderAppearsInConfig() throws Exception {
        mockMvc.perform(get("/api/market-data/ingestion-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCount", is(1)));
    }

    @Test
    void enabledProviderWithoutApiKeyCreatesFailedRun() throws Exception {
        try {
            marketDataProviderIngestionService.ingestProvider("morning");
        } catch (IllegalStateException ignored) {
            // Expected until an API key is configured.
        }

        mockMvc.perform(get("/api/market-data/ingestion-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].source", is("provider-morning-twelvedata")))
                .andExpect(jsonPath("$[0].status", is("FAILED")))
                .andExpect(jsonPath("$[0].message", is("Twelve Data API key is required when app.provider.twelvedata.enabled=true")))
                .andExpect(jsonPath("$[0].completedAt", notNullValue()));
    }
}

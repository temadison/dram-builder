package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UiResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesDashboardAndStaticModules() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DRAM Bridge Model")))
                .andExpect(content().string(containsString("href=\"/data.html\"")))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("Holdings")))
                .andExpect(content().string(containsString("Scenario")))
                .andExpect(content().string(containsString("/js/app.js")));

        mockMvc.perform(get("/data.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Market Data Workflow")))
                .andExpect(content().string(containsString("market-data-freshness")))
                .andExpect(content().string(containsString("csv-import-form")))
                .andExpect(content().string(containsString("official-nav-form")))
                .andExpect(content().string(containsString("Snapshot Entry")))
                .andExpect(content().string(containsString("market-snapshot-form")))
                .andExpect(content().string(containsString("ingestion-config-table")))
                .andExpect(content().string(containsString("run-provider-ingestion-button")))
                .andExpect(content().string(containsString("ingestion-run-table")))
                .andExpect(content().string(containsString("/js/app.js")));

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("getIngestionConfig")))
                .andExpect(content().string(containsString("getIngestionRuns")))
                .andExpect(content().string(containsString("getLatestSnapshot")))
                .andExpect(content().string(containsString("importMarketData")))
                .andExpect(content().string(containsString("importMarketDataCsv")))
                .andExpect(content().string(containsString("runProviderIngestion")))
                .andExpect(content().string(containsString("bindClick")))
                .andExpect(content().string(containsString("importSummary")))
                .andExpect(content().string(containsString("saveOfficialNavSnapshot")))
                .andExpect(content().string(containsString("saveSnapshotFromMarketData")));

        mockMvc.perform(get("/js/sampleData.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("official_nav")))
                .andExpect(content().string(containsString("ui-csv-sample")));

        mockMvc.perform(get("/js/view.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("renderIngestionConfig")))
                .andExpect(content().string(containsString("renderFreshness")))
                .andExpect(content().string(containsString("renderIngestionRuns")))
                .andExpect(content().string(containsString("freshness-status")));

        mockMvc.perform(get("/assets/bridge-mark.svg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")));
    }

    @Test
    void apiIndexListsAvailableDramEndpoints() throws Exception {
        mockMvc.perform(get("/api/dram"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("DRAM Bridge Model API")))
                .andExpect(jsonPath("$.ui", is("/")))
                .andExpect(jsonPath("$.endpoints", hasItem("GET /api/dram/latest")))
                .andExpect(jsonPath("$.endpoints", hasItem("GET /api/market-data/ingestion-config")))
                .andExpect(jsonPath("$.endpoints", hasItem("GET /api/market-data/ingestion-runs")))
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/market-data/ingest/provider")))
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/market-data/import")))
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/market-data/import/csv")))
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/market-data/official-navs")))
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/dram/snapshot/from-market-data")));
    }
}

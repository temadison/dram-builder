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
                .andExpect(content().string(containsString("Market Data Workflow")))
                .andExpect(content().string(containsString("market-snapshot-form")))
                .andExpect(content().string(containsString("/js/app.js")));

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("getLatestSnapshot")))
                .andExpect(content().string(containsString("saveSnapshotFromMarketData")));

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
                .andExpect(jsonPath("$.endpoints", hasItem("POST /api/dram/snapshot/from-market-data")));
    }
}

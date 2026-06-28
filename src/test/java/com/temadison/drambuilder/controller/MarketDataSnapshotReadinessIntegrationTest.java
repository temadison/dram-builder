package com.temadison.drambuilder.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "app.dram.snapshot.purchase-price=71.88",
        "app.dram.snapshot.etf-ticker=DRAM",
        "app.dram.snapshot.etf-exchange=BATS",
        "app.dram.snapshot.holdings[0].ticker=MU",
        "app.dram.snapshot.holdings[0].name=Micron Technology",
        "app.dram.snapshot.holdings[0].exchange=NASDAQ",
        "app.dram.snapshot.holdings[0].currency=USD",
        "app.dram.snapshot.holdings[0].weight=0.25",
        "app.dram.snapshot.holdings[1].ticker=000660",
        "app.dram.snapshot.holdings[1].name=SK hynix",
        "app.dram.snapshot.holdings[1].exchange=KRX",
        "app.dram.snapshot.holdings[1].currency=KRW",
        "app.dram.snapshot.holdings[1].weight=0.25"
})
class MarketDataSnapshotReadinessIntegrationTest {

    private static final Instant PRIOR_OBSERVED_AT = Instant.parse("2026-06-25T20:00:00Z");
    private static final Instant CURRENT_OBSERVED_AT = Instant.parse("2026-06-26T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void snapshotReadinessReportsBlockedAndReadyStates() throws Exception {
        mockMvc.perform(get("/api/market-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotReadiness.status", is("BLOCKED")))
                .andExpect(jsonPath("$.snapshotReadiness.officialNavPresent", is(false)))
                .andExpect(jsonPath("$.snapshotReadiness.issues[*].category", hasItem("OFFICIAL_NAV")))
                .andExpect(jsonPath("$.snapshotReadiness.issues[*].key", hasItem("BATS:DRAM")))
                .andExpect(jsonPath("$.snapshotReadiness.issues[*].key", hasItem("NASDAQ:MU")))
                .andExpect(jsonPath("$.snapshotReadiness.issues[*].key", hasItem("KRW/USD")));

        createOfficialNav();
        createPrice("DRAM", "Roundhill Memory ETF", "BATS", "USD", "76.89", PRIOR_OBSERVED_AT);
        createPrice("DRAM", "Roundhill Memory ETF", "BATS", "USD", "71.88", CURRENT_OBSERVED_AT);
        createPrice("MU", "Micron Technology", "NASDAQ", "USD", "104.85", PRIOR_OBSERVED_AT);
        createPrice("MU", "Micron Technology", "NASDAQ", "USD", "121.36", CURRENT_OBSERVED_AT);
        createPrice("000660", "SK hynix", "KRX", "KRW", "2580000", PRIOR_OBSERVED_AT);
        createPrice("000660", "SK hynix", "KRX", "KRW", "2917000", CURRENT_OBSERVED_AT);
        createFx("KRW", "USD", "0.000648403307", PRIOR_OBSERVED_AT);
        createFx("KRW", "USD", "0.000648130145", CURRENT_OBSERVED_AT);

        mockMvc.perform(get("/api/market-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotReadiness.status", is("READY")))
                .andExpect(jsonPath("$.snapshotReadiness.asOfDate", is("2026-06-26")))
                .andExpect(jsonPath("$.snapshotReadiness.officialNavPresent", is(true)))
                .andExpect(jsonPath("$.snapshotReadiness.issues").isEmpty());
    }

    private void createPrice(String ticker, String name, String exchange, String currency, String price, Instant observedAt)
            throws Exception {
        PriceSnapshotRequest request = new PriceSnapshotRequest(
                ticker,
                name,
                exchange,
                currency,
                new BigDecimal(price),
                "readiness-test",
                observedAt
        );
        mockMvc.perform(post("/api/market-data/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createFx(String baseCurrency, String quoteCurrency, String rate, Instant observedAt) throws Exception {
        FxRateSnapshotRequest request = new FxRateSnapshotRequest(
                baseCurrency,
                quoteCurrency,
                new BigDecimal(rate),
                "readiness-test",
                observedAt
        );
        mockMvc.perform(post("/api/market-data/fx-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void createOfficialNav() throws Exception {
        OfficialNavSnapshotRequest request = new OfficialNavSnapshotRequest(
                "DRAM",
                "Roundhill Memory ETF",
                new BigDecimal("72.02"),
                "USD",
                "readiness-test",
                LocalDate.of(2026, 6, 26),
                CURRENT_OBSERVED_AT
        );
        mockMvc.perform(post("/api/market-data/official-navs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}

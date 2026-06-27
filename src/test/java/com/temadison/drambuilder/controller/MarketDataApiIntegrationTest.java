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
import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import com.temadison.drambuilder.service.MarketDataIngestionRunService;
import java.math.BigDecimal;
import java.time.Instant;
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
class MarketDataApiIntegrationTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-06-26T20:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketDataIngestionRunService marketDataIngestionRunService;

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
                .andExpect(jsonPath("$.latestFxRates", hasSize(1)))
                .andExpect(jsonPath("$.latestOfficialNavs", hasSize(0)));
    }

    @Test
    void storesAndReadsLatestOfficialNavSnapshotAndSummary() throws Exception {
        OfficialNavSnapshotRequest request = new OfficialNavSnapshotRequest(
                "dram",
                "Roundhill Memory ETF",
                new BigDecimal("80.95"),
                "usd",
                "issuer-test",
                OBSERVED_AT.atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                OBSERVED_AT
        );

        mockMvc.perform(post("/api/market-data/official-navs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.ticker", is("DRAM")))
                .andExpect(jsonPath("$.name", is("Roundhill Memory ETF")))
                .andExpect(jsonPath("$.nav", comparesEqualTo(80.95)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.source", is("issuer-test")))
                .andExpect(jsonPath("$.asOfDate", is("2026-06-26")));

        mockMvc.perform(get("/api/market-data/official-navs/DRAM/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("DRAM")))
                .andExpect(jsonPath("$.nav", comparesEqualTo(80.95)));

        mockMvc.perform(get("/api/market-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestOfficialNavs", hasSize(1)))
                .andExpect(jsonPath("$.latestOfficialNavs[0].ticker", is("DRAM")));
    }

    @Test
    void latestPriceReturnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/market-data/prices/NASDAQ/MU/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("not_found")));
    }

    @Test
    void importsBulkMarketDataAndSupportsLatestLookups() throws Exception {
        BulkMarketDataImportRequest request = new BulkMarketDataImportRequest(
                List.of(
                        new PriceSnapshotRequest(
                                "dram",
                                "Roundhill Memory ETF",
                                "nysearca",
                                "usd",
                                new BigDecimal("81.50"),
                                "bulk-test",
                                OBSERVED_AT
                        ),
                        new PriceSnapshotRequest(
                                "000660",
                                "SK hynix",
                                "krx",
                                "krw",
                                new BigDecimal("114000.00"),
                                "bulk-test",
                                OBSERVED_AT
                        )
                ),
                List.of(new FxRateSnapshotRequest(
                        "krw",
                        "usd",
                        new BigDecimal("0.00081000"),
                        "bulk-test",
                        OBSERVED_AT
                ))
        );

        mockMvc.perform(post("/api/market-data/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricesImported", is(2)))
                .andExpect(jsonPath("$.fxRatesImported", is(1)))
                .andExpect(jsonPath("$.prices", hasSize(2)))
                .andExpect(jsonPath("$.prices[0].ticker", is("DRAM")))
                .andExpect(jsonPath("$.prices[1].ticker", is("000660")))
                .andExpect(jsonPath("$.fxRates[0].baseCurrency", is("KRW")));

        mockMvc.perform(get("/api/market-data/prices/NYSEARCA/DRAM/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", comparesEqualTo(81.50)));

        mockMvc.perform(get("/api/market-data/fx-rates/KRW/USD/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate", comparesEqualTo(0.00081000)));
    }

    @Test
    void bulkImportRejectsEmptyPayload() throws Exception {
        BulkMarketDataImportRequest request = new BulkMarketDataImportRequest(List.of(), List.of());

        mockMvc.perform(post("/api/market-data/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("At least one price or FX rate snapshot is required")));
    }

    @Test
    void importsCsvMarketDataAndSupportsLatestLookups() throws Exception {
        String csv = """
                type,ticker,name,exchange,currency,price,baseCurrency,quoteCurrency,rate,source,observedAt
                price,DRAM,Roundhill Memory ETF,NYSEARCA,USD,81.50,,,,csv-test,2026-06-26T20:00:00Z
                price,000660,SK hynix,KRX,KRW,114000,,,,csv-test,2026-06-26T20:00:00Z
                fx,,,,,,KRW,USD,0.00081000,csv-test,2026-06-26T20:00:00Z
                """;

        mockMvc.perform(post("/api/market-data/import/csv")
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricesImported", is(2)))
                .andExpect(jsonPath("$.fxRatesImported", is(1)))
                .andExpect(jsonPath("$.prices[0].ticker", is("DRAM")))
                .andExpect(jsonPath("$.prices[1].ticker", is("000660")))
                .andExpect(jsonPath("$.fxRates[0].baseCurrency", is("KRW")));

        mockMvc.perform(get("/api/market-data/prices/KRX/000660/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", comparesEqualTo(114000.0)));

        mockMvc.perform(get("/api/market-data/fx-rates/KRW/USD/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate", comparesEqualTo(0.00081000)));
    }

    @Test
    void csvImportRejectsInvalidNumericValue() throws Exception {
        String csv = """
                type,ticker,name,exchange,currency,price,source
                price,MU,Micron Technology,NASDAQ,USD,not-a-number,csv-test
                """;

        mockMvc.perform(post("/api/market-data/import/csv")
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("bad_request")))
                .andExpect(jsonPath("$.message", is("Invalid price on line 2: not-a-number")));
    }

    @Test
    void listsRecentIngestionRuns() throws Exception {
        Long runId = marketDataIngestionRunService.startFileRun("file:/tmp/dram-market-data.json").getId();
        marketDataIngestionRunService.complete(runId, 10, 2, 1, true);

        mockMvc.perform(get("/api/market-data/ingestion-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(runId.intValue())))
                .andExpect(jsonPath("$[0].source", is("file")))
                .andExpect(jsonPath("$[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$[0].requestedFile", is("file:/tmp/dram-market-data.json")))
                .andExpect(jsonPath("$[0].pricesImported", is(10)))
                .andExpect(jsonPath("$[0].fxRatesImported", is(2)))
                .andExpect(jsonPath("$[0].officialNavsImported", is(1)))
                .andExpect(jsonPath("$[0].snapshotCreated", is(true)))
                .andExpect(jsonPath("$[0].startedAt", notNullValue()))
                .andExpect(jsonPath("$[0].completedAt", notNullValue()));
    }
}

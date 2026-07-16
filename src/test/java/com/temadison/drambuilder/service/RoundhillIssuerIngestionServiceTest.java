package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RoundhillIssuerIngestionServiceTest {

    @Test
    void buildsLatestIngestionRequestFromRoundhillIssuerCsvFiles() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RoundhillIssuerIngestionService service = new RoundhillIssuerIngestionService(
                builder,
                mock(MarketDataIngestionService.class),
                "https://roundhill.test/assets/data",
                fixedClock("2026-07-01T14:00:00Z")
        );

        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_DailyNAV.csv")))
                .andRespond(withSuccess("""
                        Fund Name,Fund Ticker,CUSIP,Net Assets,Shares Outstanding,NAV,NAV Change Dollars,NAV Change Percentage,Market Price,Market Price Change Dollars,Market Price Change Percentage,Premium/Discount,Rate Date
                        Roundhill Memory ETF,DRAM,77926X320,25910762038.87,357990000.000,72.38,1.05,1.48,73.85,1.91,2.65,2.03,06/30/2026
                        """, MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_07012026.csv")))
                .andRespond(withSuccess("not found", MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_06302026.csv")))
                .andRespond(withSuccess(holdingsCsv("07/01/2026", "15.96%", "8.10%", "14.46%", "11.00%", "25.00%"), MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_06292026.csv")))
                .andRespond(withSuccess(holdingsCsv("06/30/2026", "15.50%", "8.00%", "14.00%", "10.50%", "24.00%"), MediaType.TEXT_PLAIN));

        MarketDataIngestionRequest request = service.latestIngestionRequest();

        assertThat(request.prices()).hasSize(25);
        assertThat(request.fxRates()).hasSize(8);
        assertThat(request.officialNavs()).hasSize(1);
        assertThat(request.officialNavs().getFirst().nav()).isEqualByComparingTo(new BigDecimal("72.38"));
        assertThat(request.snapshot().asOfDate()).hasToString("2026-06-30");
        assertThat(request.snapshot().purchasePrice()).isEqualByComparingTo(new BigDecimal("73.85"));
        assertThat(request.snapshot().holdings()).hasSize(12);
        assertThat(request.snapshot().holdings().stream()
                .filter(holding -> "000660".equals(holding.ticker()))
                .findFirst()
                .orElseThrow()
                .weight()).isEqualByComparingTo(new BigDecimal("0.2406"));
        assertThat(request.snapshot().holdings().stream()
                .filter(holding -> "MU".equals(holding.ticker()))
                .findFirst()
                .orElseThrow()
                .weight()).isEqualByComparingTo(new BigDecimal("0.4000"));
        server.verify();
    }

    @Test
    void usesLatestHoldingsPricesEvenWhenNavDateIsStillPriorTradingDay() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RoundhillIssuerIngestionService service = new RoundhillIssuerIngestionService(
                builder,
                mock(MarketDataIngestionService.class),
                "https://roundhill.test/assets/data",
                fixedClock("2026-07-16T17:00:00Z")
        );

        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_DailyNAV.csv")))
                .andRespond(withSuccess("""
                        Fund Name,Fund Ticker,CUSIP,Net Assets,Shares Outstanding,NAV,NAV Change Dollars,NAV Change Percentage,Market Price,Market Price Change Dollars,Market Price Change Percentage,Premium/Discount,Rate Date
                        Roundhill Memory ETF,DRAM,77926X320,25910762038.87,357990000.000,72.38,1.05,1.48,73.85,1.91,2.65,2.03,07/15/2026
                        """, MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_07162026.csv")))
                .andRespond(withSuccess(holdingsCsv("07/16/2026", "15.96%", "8.10%", "14.46%", "11.00%", "25.00%"), MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_07152026.csv")))
                .andRespond(withSuccess(holdingsCsv("07/15/2026", "15.50%", "8.00%", "14.00%", "10.50%", "24.00%"), MediaType.TEXT_PLAIN));
        server.expect(requestTo(endsWith("/FilepointRoundhill.40RU.RU_Holdings_07142026.csv")))
                .andRespond(withSuccess(holdingsCsv("07/14/2026", "15.00%", "7.90%", "13.90%", "10.40%", "23.00%"), MediaType.TEXT_PLAIN));

        MarketDataIngestionRequest request = service.latestIngestionRequest();

        assertThat(request.snapshot().asOfDate()).hasToString("2026-07-15");
        assertThat(request.prices()).anySatisfy(price -> {
            assertThat(price.ticker()).isEqualTo("000660");
            assertThat(price.exchange()).isEqualTo("KRX");
            assertThat(price.observedAt()).isEqualTo(Instant.parse("2026-07-16T20:00:00Z"));
        });
        assertThat(request.fxRates()).anySatisfy(rate -> {
            assertThat(rate.baseCurrency()).isEqualTo("KRW");
            assertThat(rate.observedAt()).isEqualTo(Instant.parse("2026-07-16T20:00:00Z"));
        });
        server.verify();
    }

    private String holdingsCsv(String date, String skWeight, String skSwapWeight, String samsungWeight, String samsungSwapWeight, String muWeight) {
        return """
                Date,Account,StockTicker,CUSIP,SecurityName,Shares,Price,MarketValue,Weightings,NetAssets,SharesOutstanding,CreationUnits,MoneyMarketFlag
                %s,DRAM,000660 KS,6450267,SK hynix Inc,100.00000000,2650000.000000,172000.00,%s,1000000.00,100000,100.000000000000,
                %s,DRAM,005930 KS,6771720,Samsung Electronics Co Ltd,100.00000000,334000.000000,21647.54,%s,1000000.00,100000,100.000000000000,
                %s,DRAM,2337 TT,6574101,Macronix International Co Ltd,100.00000000,155.500000,488.15,0.35%%,1000000.00,100000,100.000000000000,
                %s,DRAM,2344 TT,6966515,Winbond Electronics Corp,100.00000000,207.500000,651.41,1.11%%,1000000.00,100000,100.000000000000,
                %s,DRAM,2408 TT,6283601,Nanya Technology Corp,100.00000000,452.500000,1420.67,1.81%%,1000000.00,100000,100.000000000000,
                %s,DRAM,285A JP,BMGYJ02,Kioxia Holdings Corp,100.00000000,89680.000000,55470.60,4.36%%,1000000.00,100000,100.000000000000,
                %s,DRAM,603986 C1,BHWLWF8,GigaDevice Semiconductor Inc,100.00000000,760.000000,11172.00,2.91%%,1000000.00,100000,100.000000000000,
                %s,DRAM,8299 TT,6728469,Phison Electronics Corp,100.00000000,2310.000000,7251.60,0.61%%,1000000.00,100000,100.000000000000,
                %s,DRAM,595112103 TRS 050427 NM,595112103 TRS 050427 NM,MICRON TECHNOLOGY INC SWAP NM,100.00000000,100.000000,100.00,10.00%%,1000000.00,100000,100.000000000000,
                %s,DRAM,595112103 TRS 052427 GS,595112103 TRS 052427 GS,"MICRON TECHNOLOGY, INC.-SWAP-GOLD-L",100.00000000,100.000000,100.00,5.00%%,1000000.00,100000,100.000000000000,
                %s,DRAM,6450267 TRS 052427 GS,6450267 TRS 052427 GS,SK HYNIX INC-SWAP-GOLD-L,100.00000000,100.000000,100.00,%s,1000000.00,100000,100.000000000000,
                %s,DRAM,6771720 TRS 052427 GS,6771720 TRS 052427 GS,SAMSUNG ELECTRONICS -SWAP-GOLD-L,100.00000000,100.000000,100.00,%s,1000000.00,100000,100.000000000000,
                %s,DRAM,MU,595112103,Micron Technology Inc,100.00000000,121.360000,12136.00,%s,1000000.00,100000,100.000000000000,
                %s,DRAM,SNDK,80004C200,Sandisk Corp,100.00000000,233.500000,23350.00,5.00%%,1000000.00,100000,100.000000000000,
                %s,DRAM,STX,G7997R103,Seagate Technology Holdings PLC,100.00000000,102.536000,10253.60,4.16%%,1000000.00,100000,100.000000000000,
                %s,DRAM,WDC,958102105,Western Digital Corp,100.00000000,67.539000,6753.90,4.24%%,1000000.00,100000,100.000000000000,
                """.formatted(
                date, skWeight,
                date, samsungWeight,
                date,
                date,
                date,
                date,
                date,
                date,
                date,
                date,
                date, skSwapWeight,
                date, samsungSwapWeight,
                date, muWeight,
                date,
                date,
                date
        );
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("America/New_York"));
    }
}

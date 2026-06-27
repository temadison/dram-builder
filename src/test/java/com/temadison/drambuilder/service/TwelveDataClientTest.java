package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.temadison.drambuilder.config.TwelveDataProviderProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TwelveDataClientTest {

    @Test
    void fetchesDailyClosesFromTimeSeriesEndpoint() {
        TwelveDataProviderProperties properties = new TwelveDataProviderProperties();
        properties.setApiKey("test-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwelveDataClient client = new TwelveDataClient(builder, properties);

        server.expect(requestTo(startsWith("https://api.twelvedata.com/time_series")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("symbol", "MU"))
                .andExpect(queryParam("exchange", "NASDAQ"))
                .andExpect(queryParam("interval", "1day"))
                .andExpect(queryParam("outputsize", "2"))
                .andExpect(queryParam("apikey", "test-key"))
                .andRespond(withSuccess("""
                        {
                          "values": [
                            { "datetime": "2026-06-01", "close": "103.55" },
                            { "datetime": "2026-05-29", "close": "97.10" }
                          ],
                          "status": "ok"
                        }
                        """, MediaType.APPLICATION_JSON));

        List<TwelveDataClient.DailyClose> closes = client.dailyCloses("MU", "NASDAQ", 2);

        assertThat(closes).hasSize(2);
        assertThat(closes.get(0).observedAt()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(closes.get(0).close()).isEqualByComparingTo(new BigDecimal("103.55"));
        assertThat(closes.get(1).observedAt()).isEqualTo(Instant.parse("2026-05-29T00:00:00Z"));
        assertThat(closes.get(1).close()).isEqualByComparingTo(new BigDecimal("97.10"));
        server.verify();
    }
}

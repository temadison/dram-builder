package com.temadison.drambuilder.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-data.freshness")
public class MarketDataFreshnessProperties {

    private long maxAgeHours = 18;
    private String marketZone = "America/New_York";
    private String expectedAfterLocalTime = "17:30";
    private String marketHolidays = "";
    private String requiredPrices = "BZX:DRAM,NASDAQ:MU,NASDAQ:SNDK,NASDAQ:WDC,NASDAQ:STX";
    private Map<String, ExchangeCalendar> exchangeCalendars = new LinkedHashMap<>();

    public long getMaxAgeHours() {
        return maxAgeHours;
    }

    public void setMaxAgeHours(long maxAgeHours) {
        this.maxAgeHours = maxAgeHours;
    }

    public String getMarketZone() {
        return marketZone;
    }

    public void setMarketZone(String marketZone) {
        this.marketZone = marketZone;
    }

    public String getExpectedAfterLocalTime() {
        return expectedAfterLocalTime;
    }

    public void setExpectedAfterLocalTime(String expectedAfterLocalTime) {
        this.expectedAfterLocalTime = expectedAfterLocalTime;
    }

    public String getMarketHolidays() {
        return marketHolidays;
    }

    public void setMarketHolidays(String marketHolidays) {
        this.marketHolidays = marketHolidays;
    }

    public String getRequiredPrices() {
        return requiredPrices;
    }

    public void setRequiredPrices(String requiredPrices) {
        this.requiredPrices = requiredPrices;
    }

    public Map<String, ExchangeCalendar> getExchangeCalendars() {
        return exchangeCalendars;
    }

    public void setExchangeCalendars(Map<String, ExchangeCalendar> exchangeCalendars) {
        this.exchangeCalendars = exchangeCalendars == null ? new LinkedHashMap<>() : exchangeCalendars;
    }

    public static class ExchangeCalendar {

        private String marketZone;
        private String expectedAfterLocalTime;
        private String marketHolidays = "";

        public String getMarketZone() {
            return marketZone;
        }

        public void setMarketZone(String marketZone) {
            this.marketZone = marketZone;
        }

        public String getExpectedAfterLocalTime() {
            return expectedAfterLocalTime;
        }

        public void setExpectedAfterLocalTime(String expectedAfterLocalTime) {
            this.expectedAfterLocalTime = expectedAfterLocalTime;
        }

        public String getMarketHolidays() {
            return marketHolidays;
        }

        public void setMarketHolidays(String marketHolidays) {
            this.marketHolidays = marketHolidays;
        }
    }
}

package com.temadison.drambuilder.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dram.snapshot")
public class DramSnapshotProperties {

    private boolean enabled;
    private BigDecimal purchasePrice;
    private String etfTicker = "DRAM";
    private String etfExchange = "BATS";
    private List<Holding> holdings = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getEtfTicker() {
        return etfTicker;
    }

    public void setEtfTicker(String etfTicker) {
        this.etfTicker = etfTicker;
    }

    public String getEtfExchange() {
        return etfExchange;
    }

    public void setEtfExchange(String etfExchange) {
        this.etfExchange = etfExchange;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holding> holdings) {
        this.holdings = holdings == null ? new ArrayList<>() : holdings;
    }

    public static class Holding {

        private String ticker;
        private String name;
        private String exchange;
        private String currency;
        private BigDecimal weight;

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }
    }
}

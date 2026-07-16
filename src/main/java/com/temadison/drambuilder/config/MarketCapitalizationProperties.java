package com.temadison.drambuilder.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.market-cap")
public class MarketCapitalizationProperties {

    private BigDecimal micronSharesOutstanding = new BigDecimal("1116000000");
    private BigDecimal skHynixLocalSharesOutstanding = new BigDecimal("728002365");
    private int skHynixAdrPerLocalShare = 10;

    public BigDecimal getMicronSharesOutstanding() {
        return micronSharesOutstanding;
    }

    public void setMicronSharesOutstanding(BigDecimal micronSharesOutstanding) {
        this.micronSharesOutstanding = micronSharesOutstanding;
    }

    public BigDecimal getSkHynixLocalSharesOutstanding() {
        return skHynixLocalSharesOutstanding;
    }

    public void setSkHynixLocalSharesOutstanding(BigDecimal skHynixLocalSharesOutstanding) {
        this.skHynixLocalSharesOutstanding = skHynixLocalSharesOutstanding;
    }

    public int getSkHynixAdrPerLocalShare() {
        return skHynixAdrPerLocalShare;
    }

    public void setSkHynixAdrPerLocalShare(int skHynixAdrPerLocalShare) {
        this.skHynixAdrPerLocalShare = skHynixAdrPerLocalShare;
    }
}

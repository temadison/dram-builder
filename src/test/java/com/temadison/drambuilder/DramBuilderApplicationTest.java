package com.temadison.drambuilder;

import static org.assertj.core.api.Assertions.assertThat;

import com.temadison.drambuilder.service.AttributionCalculator;
import com.temadison.drambuilder.service.BridgeScoreCalculator;
import com.temadison.drambuilder.service.DramBridgeScoreService;
import com.temadison.drambuilder.service.DramScenarioService;
import com.temadison.drambuilder.service.DramSnapshotService;
import com.temadison.drambuilder.service.MarketDataService;
import com.temadison.drambuilder.service.ScenarioCalculator;
import com.temadison.drambuilder.service.SyntheticNavCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class DramBuilderApplicationTest {

    @Autowired
    private DramSnapshotService dramSnapshotService;

    @Autowired
    private DramScenarioService dramScenarioService;

    @Autowired
    private DramBridgeScoreService dramBridgeScoreService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private SyntheticNavCalculator syntheticNavCalculator;

    @Autowired
    private AttributionCalculator attributionCalculator;

    @Autowired
    private ScenarioCalculator scenarioCalculator;

    @Autowired
    private BridgeScoreCalculator bridgeScoreCalculator;

    @Test
    void contextLoadsCoreServicesThroughInterfaces() {
        assertThat(dramSnapshotService).isNotNull();
        assertThat(dramScenarioService).isNotNull();
        assertThat(dramBridgeScoreService).isNotNull();
        assertThat(marketDataService).isNotNull();
        assertThat(syntheticNavCalculator).isNotNull();
        assertThat(attributionCalculator).isNotNull();
        assertThat(scenarioCalculator).isNotNull();
        assertThat(bridgeScoreCalculator).isNotNull();
    }
}

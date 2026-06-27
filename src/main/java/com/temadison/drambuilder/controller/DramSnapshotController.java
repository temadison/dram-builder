package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.ApiEndpointResponse;
import com.temadison.drambuilder.dto.BridgeScoreRequest;
import com.temadison.drambuilder.dto.BridgeScoreResponse;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.ScenarioResponse;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.service.DramBridgeScoreService;
import com.temadison.drambuilder.service.DramMarketDataSnapshotService;
import com.temadison.drambuilder.service.DramScenarioService;
import com.temadison.drambuilder.service.DramSnapshotService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dram")
public class DramSnapshotController {

    private final DramSnapshotService dramSnapshotService;
    private final DramMarketDataSnapshotService dramMarketDataSnapshotService;
    private final DramScenarioService dramScenarioService;
    private final DramBridgeScoreService dramBridgeScoreService;

    public DramSnapshotController(
            DramSnapshotService dramSnapshotService,
            DramMarketDataSnapshotService dramMarketDataSnapshotService,
            DramScenarioService dramScenarioService,
            DramBridgeScoreService dramBridgeScoreService
    ) {
        this.dramSnapshotService = dramSnapshotService;
        this.dramMarketDataSnapshotService = dramMarketDataSnapshotService;
        this.dramScenarioService = dramScenarioService;
        this.dramBridgeScoreService = dramBridgeScoreService;
    }

    @GetMapping
    public ApiEndpointResponse apiIndex() {
        return new ApiEndpointResponse(
                "DRAM Bridge Model API",
                "/",
                List.of(
                        "GET /api/dram/latest",
                        "POST /api/dram/snapshot",
                        "POST /api/dram/snapshot/from-market-data",
                        "POST /api/dram/scenario",
                        "GET /api/dram/bridge-score",
                        "POST /api/dram/bridge-score",
                        "GET /api/market-data",
                        "GET /api/market-data/ingestion-runs",
                        "POST /api/market-data/import",
                        "POST /api/market-data/import/csv",
                        "POST /api/market-data/prices",
                        "POST /api/market-data/fx-rates",
                        "POST /api/market-data/official-navs"
                )
        );
    }

    @PostMapping("/snapshot")
    public SnapshotResponse createSnapshot(@Valid @RequestBody SnapshotRequest request) {
        return dramSnapshotService.createSnapshot(request);
    }

    @PostMapping("/snapshot/from-market-data")
    public SnapshotResponse createSnapshotFromMarketData(@Valid @RequestBody MarketDataSnapshotRequest request) {
        return dramMarketDataSnapshotService.createSnapshot(request);
    }

    @GetMapping("/latest")
    public SnapshotResponse latestSnapshot() {
        return dramSnapshotService.latestSnapshot();
    }

    @PostMapping("/scenario")
    public ScenarioResponse runScenario(@Valid @RequestBody ScenarioRequest request) {
        return dramScenarioService.runScenario(request);
    }

    @GetMapping("/bridge-score")
    public BridgeScoreResponse latestBridgeScore() {
        return dramBridgeScoreService.latestBridgeScore();
    }

    @PostMapping("/bridge-score")
    public BridgeScoreResponse latestBridgeScore(@Valid @RequestBody BridgeScoreRequest request) {
        return dramBridgeScoreService.latestBridgeScore(request);
    }
}

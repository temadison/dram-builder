package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.BridgeScoreRequest;
import com.temadison.drambuilder.dto.BridgeScoreResponse;
import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.ScenarioResponse;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.service.DramBridgeScoreService;
import com.temadison.drambuilder.service.DramScenarioService;
import com.temadison.drambuilder.service.DramSnapshotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dram")
public class DramSnapshotController {

    private final DramSnapshotService dramSnapshotService;
    private final DramScenarioService dramScenarioService;
    private final DramBridgeScoreService dramBridgeScoreService;

    public DramSnapshotController(
            DramSnapshotService dramSnapshotService,
            DramScenarioService dramScenarioService,
            DramBridgeScoreService dramBridgeScoreService
    ) {
        this.dramSnapshotService = dramSnapshotService;
        this.dramScenarioService = dramScenarioService;
        this.dramBridgeScoreService = dramBridgeScoreService;
    }

    @PostMapping("/snapshot")
    public SnapshotResponse createSnapshot(@Valid @RequestBody SnapshotRequest request) {
        return dramSnapshotService.createSnapshot(request);
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

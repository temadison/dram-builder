package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.ScenarioResponse;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
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

    public DramSnapshotController(DramSnapshotService dramSnapshotService) {
        this.dramSnapshotService = dramSnapshotService;
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
        return dramSnapshotService.runScenario(request);
    }
}

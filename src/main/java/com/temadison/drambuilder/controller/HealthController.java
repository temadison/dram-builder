package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.HealthResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "dram-bridge-model", Instant.now());
    }
}

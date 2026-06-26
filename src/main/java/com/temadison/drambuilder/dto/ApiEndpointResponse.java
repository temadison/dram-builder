package com.temadison.drambuilder.dto;

import java.util.List;

public record ApiEndpointResponse(
        String name,
        String ui,
        List<String> endpoints
) {
}

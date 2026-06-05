package com.synapse.learning.srs.adapter.in.web.dto;

import java.util.List;

public record RetentionStatsResponse(
        List<RetentionPointResponse> points) {
}

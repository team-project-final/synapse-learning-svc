package com.synapse.learning.srs.application.port.in;

import com.synapse.learning.srs.adapter.in.web.dto.ReviewStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.WeeklyStatsResponse;

public interface ReviewStatsUseCase {
    ReviewStatsResponse getOverview(String tenantId, String userId);

    WeeklyStatsResponse getHeatmap(String tenantId, String userID);

}

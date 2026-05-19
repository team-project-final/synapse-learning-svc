package com.synapse.learning.srs.application.port.in;

import com.synapse.learning.srs.adapter.in.web.dto.ReviewCardResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionStartRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSessionSubmitRequest;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewSubmitResponse;

import java.util.List;
import java.util.UUID;

public interface ReviewSessionUseCase {

    ReviewSessionResponse startSession(String tenantId, String userId, ReviewSessionStartRequest request);

    List<ReviewCardResponse> getReviewQueue(String tenantId, UUID deckId);

    ReviewSubmitResponse submitReview(String tenantId, String userId, UUID sessionId, ReviewSessionSubmitRequest request);

    ReviewSessionResponse completeSession(String tenantId, UUID sessionId);
}

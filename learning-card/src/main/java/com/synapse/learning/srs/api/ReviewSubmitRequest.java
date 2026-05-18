package com.synapse.learning.srs.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewSubmitRequest(

        @NotNull(message = "rating은 필수입니다") @Min(value = 1, message = "rating은 1 이상이어야 합니다") @Max(value = 4, message = "rating은 4 이하이어야 합니다") Integer rating) {
}
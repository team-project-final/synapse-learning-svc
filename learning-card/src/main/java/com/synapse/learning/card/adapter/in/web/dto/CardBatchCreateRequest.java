package com.synapse.learning.card.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CardBatchCreateRequest(
        @NotEmpty @Size(max = 100) List<@Valid CardCreateRequest> cards) {
}

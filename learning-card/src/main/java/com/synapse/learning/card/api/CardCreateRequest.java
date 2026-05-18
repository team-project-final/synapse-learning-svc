package com.synapse.learning.card.api;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardCreateRequest(

        @NotBlank(message = "앞면 내용은 필수입니다") String frontContent,

        @NotBlank(message = "뒷면 내용은 필수입니다") String backContent,

        @NotBlank(message = "카드 타입은 필수입니다") @Size(max = 20) String cardType,

        UUID sourceId,

        @Size(max = 20) String bloomLevel) {
}
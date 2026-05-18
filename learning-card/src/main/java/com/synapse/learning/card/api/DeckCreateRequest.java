package com.synapse.learning.card.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeckCreateRequest(

        @NotBlank(message = "덱 이름은 필수입니다") @Size(max = 100, message = "덱 이름은 100자 이하여야 합니다") String name,

        @Size(max = 500, message = "설명은 500자 이하여야 합니다") String description,

        @Size(min = 7, max = 7, message = "색상은 #RRGGBB 형식이어야 합니다") String color) {
}
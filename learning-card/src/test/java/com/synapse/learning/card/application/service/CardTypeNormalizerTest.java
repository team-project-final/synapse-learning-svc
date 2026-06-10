package com.synapse.learning.card.application.service;

import com.synapse.learning.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTypeNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {"BASIC", "basic", "qa", "QA", "AI_GENERATED", "ai_generated"})
    @DisplayName("basic 계열 입력은 모두 'basic'으로 정규화")
    void normalize_toBasic(String input) {
        assertThat(CardTypeNormalizer.normalize(input)).isEqualTo("basic");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOZE", "cloze"})
    @DisplayName("cloze 계열 입력은 'cloze'로 정규화")
    void normalize_toCloze(String input) {
        assertThat(CardTypeNormalizer.normalize(input)).isEqualTo("cloze");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEFINITION", "definition"})
    @DisplayName("definition 계열 입력은 'definition'으로 정규화")
    void normalize_toDefinition(String input) {
        assertThat(CardTypeNormalizer.normalize(input)).isEqualTo("definition");
    }

    @Test
    @DisplayName("알 수 없는 값은 VALIDATION_ERROR")
    void normalize_unknown_throwsException() {
        assertThatThrownBy(() -> CardTypeNormalizer.normalize("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null 입력은 VALIDATION_ERROR")
    void normalize_null_throwsException() {
        assertThatThrownBy(() -> CardTypeNormalizer.normalize(null))
                .isInstanceOf(BusinessException.class);
    }
}

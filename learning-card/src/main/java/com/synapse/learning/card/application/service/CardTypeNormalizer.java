package com.synapse.learning.card.application.service;

import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;

public class CardTypeNormalizer {

    private CardTypeNormalizer() {}

    /**
     * 외부 입력값을 DB canonical 소문자 값으로 정규화한다.
     * Flutter: BASIC/CLOZE, 레거시: qa/AI_GENERATED 모두 수용.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "cardType은 필수입니다");
        }
        return switch (raw.toUpperCase()) {
            case "BASIC", "QA", "AI_GENERATED" -> "basic";
            case "CLOZE" -> "cloze";
            case "DEFINITION" -> "definition";
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "알 수 없는 cardType: " + raw);
        };
    }
}

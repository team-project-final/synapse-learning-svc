package com.synapse.learning.card.domain.exception;

import com.synapse.learning.global.exception.BusinessException;
import com.synapse.learning.global.exception.ErrorCode;

public class CardNotFoundException extends BusinessException {

    public CardNotFoundException(String cardId) {
        super(ErrorCode.CARD_NOT_FOUND, "카드를 찾을 수 없습니다: " + cardId);
    }
}
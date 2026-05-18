package com.synapse.learning.card.domain.exception;

import com.synapse.learning.shared.exception.BusinessException;
import com.synapse.learning.shared.exception.ErrorCode;

public class DeckNotFoundException extends BusinessException {

    public DeckNotFoundException(String deckId) {
        super(ErrorCode.DECK_NOT_FOUND, "덱을 찾을 수 없습니다: " + deckId);
    }
}
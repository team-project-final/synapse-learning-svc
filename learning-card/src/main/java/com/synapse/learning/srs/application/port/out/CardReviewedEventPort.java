package com.synapse.learning.srs.application.port.out;

public interface CardReviewedEventPort {

    void publish(String userId, String cardId, String deckId, int rating);
}

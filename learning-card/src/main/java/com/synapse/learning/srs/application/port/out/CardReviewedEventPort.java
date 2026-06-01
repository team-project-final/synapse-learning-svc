package com.synapse.learning.srs.application.port.out;

public interface CardReviewedEventPort {

    /**
     * 복습 완료 이벤트를 Kafka에 발행한다.
     *
     * @param userId       복습한 사용자 UUID
     * @param tenantId     테넌트 ID
     * @param cardId       복습한 카드 UUID
     * @param rating       복습 평가 (1=AGAIN, 2=HARD, 3=GOOD, 4=EASY)
     * @param nextReviewAt 다음 복습 일자 (ISO-8601, SM-2 계산 결과)
     */
    void publish(String userId, String tenantId, String cardId, int rating, String nextReviewAt);
}

package com.synapse.learning.srs.application.port.out;

public interface ReviewDueEventPort {

    /**
     * card.review.due 이벤트를 Kafka에 발행한다.
     *
     * @param userId       복습 대상 사용자 UUID
     * @param tenantId     테넌트 ID
     * @param dueCardCount 오늘 복습할 카드 수
     * @param dueDate      기준 날짜 (yyyy-MM-dd)
     */
    void publish(String userId, String tenantId, int dueCardCount, String dueDate);
}

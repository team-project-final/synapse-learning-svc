package com.synapse.learning.srs.adapter.out.engagement;

import org.springframework.stereotype.Component;

import com.synapse.learning.srs.application.port.out.StreakPort;

// TODO: engagement-svc Kafka 연동 확정 후 KafkaStreakAdapter로 교체
@Component
public class MockStreakAdapter implements StreakPort {

    @Override
    public StreakData getStreak(String userId, String tenantId) {
        return new StreakData(0, 0);
    }

}

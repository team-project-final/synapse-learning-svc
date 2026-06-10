package com.synapse.learning.srs.adapter.out.engagement;

import com.synapse.learning.srs.application.port.out.StreakPort;
import org.springframework.stereotype.Component;

// Intentional fallback until engagement-svc exposes a read contract for streaks.
@Component
public class MockStreakAdapter implements StreakPort {

    @Override
    public StreakData getStreak(String userId, String tenantId) {
        return new StreakData(0, 0);
    }
}

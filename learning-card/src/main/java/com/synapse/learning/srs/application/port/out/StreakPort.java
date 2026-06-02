package com.synapse.learning.srs.application.port.out;

public interface StreakPort {

    record StreakData(int currentStreak, int longestStreak) {
    }

    StreakData getStreak(String userId, String tenantId);

}

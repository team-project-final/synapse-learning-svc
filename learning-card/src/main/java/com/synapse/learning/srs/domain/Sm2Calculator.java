package com.synapse.learning.srs.domain;

import org.springframework.stereotype.Component;

@Component
public class Sm2Calculator {

    private static final double MIN_EASE_FACTOR = 1.3;

    /**
     * SM-2 계산
     * rating: 1=Again, 2=Hard, 3=Good, 4=Easy
     * 내부적으로 quality로 변환: 1→1, 2→2, 3→4, 4→5
     */
    public Sm2Result calculate(int rating, double easeFactor, int intervalDays, int repetitions) {
        int quality = toQuality(rating);

        // easeFactor 업데이트
        double newEF = easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        newEF = Math.max(MIN_EASE_FACTOR, newEF);
        newEF = Math.round(newEF * 100.0) / 100.0;

        // interval 및 repetitions 계산
        int newInterval;
        int newRepetitions;

        if (quality < 3) {
            // rating 1, 2 → 다시 처음부터
            newInterval = 1;
            newRepetitions = 0;
        } else {
            // rating 3, 4 → 정상 진행
            newRepetitions = repetitions + 1;
            if (repetitions == 0) {
                newInterval = 1;
            } else if (repetitions == 1) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(intervalDays * newEF);
            }
        }

        return new Sm2Result(newEF, newInterval, newRepetitions);
    }

    private int toQuality(int rating) {
        return switch (rating) {
            case 1 -> 1; // Again
            case 2 -> 2; // Hard
            case 3 -> 4; // Good
            case 4 -> 5; // Easy
            default -> throw new IllegalArgumentException("rating은 1~4 사이여야 합니다: " + rating);
        };
    }
}
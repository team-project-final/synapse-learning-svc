package com.synapse.learning.srs.domain;

import org.springframework.stereotype.Component;

@Component
public class Sm2Calculator {

    private static final double MIN_EASE_FACTOR = 1.3;

    public Sm2Result calculate(int rating, double easeFactor, int intervalDays, int repetitions) {
        // AGAIN (실패) — 별도 처리
        if (rating == 1) {
            double newEF = Math.max(MIN_EASE_FACTOR, easeFactor - 0.2);
            newEF = Math.round(newEF * 100.0) / 100.0;
            return new Sm2Result(newEF, 0, 0); // interval=0 → 10분 후
        }

        // 성공 (HARD=2, GOOD=3, EASY=4)
        int newRepetitions = repetitions + 1;
        double newEF = easeFactor + (0.1 - (4 - rating) * (0.08 + (4 - rating) * 0.02));
        newEF = Math.max(MIN_EASE_FACTOR, newEF);
        newEF = Math.round(newEF * 100.0) / 100.0;

        int newInterval;
        if (newRepetitions == 1)
            newInterval = 1;
        else if (newRepetitions == 2)
            newInterval = 6;
        else
            newInterval = (int) Math.round(intervalDays * newEF);

        return new Sm2Result(newEF, newInterval, newRepetitions);
    }
}
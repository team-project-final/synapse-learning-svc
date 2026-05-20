package com.synapse.learning.srs.domain;

import org.springframework.stereotype.Component;

@Component
public class Sm2Calculator {

    private static final double MIN_EASE_FACTOR = 1.3;

    public Sm2Result calculate(int rating, double easeFactor, int intervalDays, int repetitions) {
        if (rating == 1) {
            double newEF = Math.max(MIN_EASE_FACTOR, easeFactor - 0.2);
            newEF = Math.round(newEF * 100.0) / 100.0;
            return new Sm2Result(newEF, 1, 0);
        }

        int newRepetitions = repetitions + 1;
        double newEF = easeFactor + (0.1 - (4 - rating) * (0.08 + (4 - rating) * 0.02));
        newEF = Math.max(MIN_EASE_FACTOR, newEF);
        newEF = Math.round(newEF * 100.0) / 100.0;

        int newInterval;
        if (rating == 2) {
            newInterval = Math.max(1, intervalDays);
        } else if (rating == 3) {
            newInterval = intervalDays == 0
                    ? 1
                    : Math.max(1, (int) Math.round(intervalDays * newEF));
        } else if (rating == 4) {
            newInterval = intervalDays == 0
                    ? 1
                    : Math.max(1, (int) Math.round(intervalDays * newEF * 2));
        } else {
            throw new IllegalArgumentException("유효하지 않은 rating: " + rating);
        }

        return new Sm2Result(newEF, newInterval, newRepetitions);
    }
}

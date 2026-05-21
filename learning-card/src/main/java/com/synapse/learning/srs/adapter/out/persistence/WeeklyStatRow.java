package com.synapse.learning.srs.adapter.out.persistence;

import java.time.LocalDate;

public interface WeeklyStatRow {

    LocalDate getWeekStart();

    long getReviewCount();

    long getCorrectCount();

}

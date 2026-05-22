package com.synapse.learning.srs.adapter.out.persistence;

import java.time.LocalDate;

public interface DailyStatRow {
    LocalDate getReviewDate();

    long getReviewCount();

    long getCorrectCount();

}

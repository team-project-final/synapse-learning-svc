package com.synapse.learning.srs.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2CalculatorTest {

    private Sm2Calculator sm2Calculator;

    @BeforeEach
    void setUp() {
        sm2Calculator = new Sm2Calculator();
    }

    // ── AGAIN (rating=1) ─────────────────────────────

    @Test
    @DisplayName("AGAIN: interval=0, repetitions=0, EF 감소")
    void again_resetsIntervalAndRepetitions() {
        Sm2Result result = sm2Calculator.calculate(1, 2.5, 10, 3);

        assertThat(result.intervalDays()).isEqualTo(0);
        assertThat(result.repetitions()).isEqualTo(0);
        assertThat(result.easeFactor()).isEqualTo(2.3);
    }

    @Test
    @DisplayName("AGAIN: EF가 1.3 미만으로 내려가지 않음")
    void again_efDoesNotGoBelowMinimum() {
        Sm2Result result = sm2Calculator.calculate(1, 1.3, 0, 0);

        assertThat(result.easeFactor()).isEqualTo(1.3);
    }

    // ── HARD (rating=2) ──────────────────────────────

    @Test
    @DisplayName("HARD: 첫 번째 성공 → interval=1, repetitions=1")
    void hard_firstRepetition() {
        Sm2Result result = sm2Calculator.calculate(2, 2.5, 0, 0);

        assertThat(result.repetitions()).isEqualTo(1);
        assertThat(result.intervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("HARD: EF 감소 확인 (2.5 → 2.36)")
    void hard_efDecreases() {
        Sm2Result result = sm2Calculator.calculate(2, 2.5, 0, 0);

        assertThat(result.easeFactor()).isEqualTo(2.36);
    }

    // ── GOOD (rating=3) ──────────────────────────────

    @Test
    @DisplayName("GOOD: 첫 번째 성공 → interval=1, repetitions=1")
    void good_firstRepetition() {
        Sm2Result result = sm2Calculator.calculate(3, 2.5, 0, 0);

        assertThat(result.repetitions()).isEqualTo(1);
        assertThat(result.intervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("GOOD: 두 번째 성공 → interval=6, repetitions=2")
    void good_secondRepetition() {
        Sm2Result result = sm2Calculator.calculate(3, 2.5, 1, 1);

        assertThat(result.repetitions()).isEqualTo(2);
        assertThat(result.intervalDays()).isEqualTo(6);
    }

    @Test
    @DisplayName("GOOD: 세 번째 이후 → interval = prevInterval * EF")
    void good_thirdRepetitionAndBeyond() {
        Sm2Result result = sm2Calculator.calculate(3, 2.5, 6, 2);

        assertThat(result.repetitions()).isEqualTo(3);
        assertThat(result.intervalDays()).isEqualTo(15); // 6 * 2.5 = 15
    }

    @Test
    @DisplayName("GOOD: EF 변화 없음 (2.5 유지)")
    void good_efUnchanged() {
        Sm2Result result = sm2Calculator.calculate(3, 2.5, 0, 0);

        assertThat(result.easeFactor()).isEqualTo(2.5);
    }

    // ── EASY (rating=4) ──────────────────────────────

    @Test
    @DisplayName("EASY: 첫 번째 성공 → interval=1, repetitions=1")
    void easy_firstRepetition() {
        Sm2Result result = sm2Calculator.calculate(4, 2.5, 0, 0);

        assertThat(result.repetitions()).isEqualTo(1);
        assertThat(result.intervalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("EASY: EF 증가 확인 (2.5 → 2.6)")
    void easy_efIncreases() {
        Sm2Result result = sm2Calculator.calculate(4, 2.5, 0, 0);

        assertThat(result.easeFactor()).isEqualTo(2.6);
    }

    // ── EF 최솟값 경계 ────────────────────────────────

    @Test
    @DisplayName("HARD 반복: EF가 1.3 미만으로 내려가지 않음")
    void hard_efDoesNotGoBelowMinimum() {
        Sm2Result result = sm2Calculator.calculate(2, 1.3, 1, 1);

        assertThat(result.easeFactor()).isGreaterThanOrEqualTo(1.3);
    }
}
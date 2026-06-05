package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.adapter.out.persistence.FlashCardJpaRepository;
import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewDueScheduler 대량 사용자 시뮬레이션 (1000명)")
class ReviewDueSchedulerLoadTest {

    private static final int TOTAL_USERS = 1000;
    private static final int BATCH_SIZE = 100;
    private static final long MAX_PROCESSING_MS = 3_000;
    private static final String TENANT_ID = UUID.randomUUID().toString();

    @Mock
    FlashCardJpaRepository flashCardJpaRepository;

    @Mock
    ReviewDueEventPort reviewDueEventPort;

    @InjectMocks
    ReviewDueScheduler scheduler;

    @Test
    @DisplayName("1000명 처리 시 이벤트가 전원 발행되고 3초 이내에 완료된다")
    void loadTest_1000users_allPublishedWithinTimeLimit() {
        given(flashCardJpaRepository.findDueCardCountByUser(eq(BATCH_SIZE), anyInt()))
                .willAnswer(invocation -> {
                    int offset = invocation.getArgument(1);
                    if (offset >= TOTAL_USERS) return Collections.emptyList();
                    return createBatch(offset);
                });

        long start = System.currentTimeMillis();
        scheduler.publishDueEvents();
        long elapsed = System.currentTimeMillis() - start;

        verify(reviewDueEventPort, times(TOTAL_USERS))
                .publish(anyString(), eq(TENANT_ID), anyInt(), anyString());
        assertThat(elapsed)
                .as("1000명 처리 시간이 %dms를 초과함: %dms", MAX_PROCESSING_MS, elapsed)
                .isLessThan(MAX_PROCESSING_MS);
    }

    private List<Object[]> createBatch(int offset) {
        return IntStream.range(0, BATCH_SIZE)
                .mapToObj(i -> new Object[]{"user-" + (offset + i), TENANT_ID, 5})
                .toList();
    }
}

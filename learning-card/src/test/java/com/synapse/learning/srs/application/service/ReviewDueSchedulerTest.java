package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.adapter.out.persistence.FlashCardJpaRepository;
import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewDueSchedulerTest {

    @Mock
    private FlashCardJpaRepository flashCardJpaRepository;

    @Mock
    private ReviewDueEventPort reviewDueEventPort;

    @InjectMocks
    private ReviewDueScheduler scheduler;

    private Object[] row(String userId, int count) {
        return new Object[] { userId, count };
    }

    @Test
    @DisplayName("복습 대상 사용자가 있으면 이벤트를 발행한다")
    void publishDueEvents_whenUsersExist_publishesEvents() {
        // given
        List<Object[]> batch = List.of(
                row("user-1", 5),
                row("user-2", 3));
        given(flashCardJpaRepository.findDueCardCountByUser(100, 0)).willReturn(batch);

        // when
        scheduler.publishDueEvents();

        // then
        verify(reviewDueEventPort, times(1)).publish(eq("user-1"), eq(5), anyString());
        verify(reviewDueEventPort, times(1)).publish(eq("user-2"), eq(3), anyString());
        verify(reviewDueEventPort, times(2)).publish(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("복습 대상 사용자가 없으면 이벤트를 발행하지 않는다")
    void publishDueEvents_whenNoUsers_doesNotPublish() {
        // given
        given(flashCardJpaRepository.findDueCardCountByUser(anyInt(), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        scheduler.publishDueEvents();

        // then
        verify(reviewDueEventPort, never()).publish(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("이벤트 발행 중 예외가 발생해도 다른 사용자는 계속 처리한다")
    void publishDueEvents_whenOneUserFails_continuesOthers() {
        // given
        List<Object[]> batch = List.of(
                row("user-1", 5),
                row("user-2", 3));
        given(flashCardJpaRepository.findDueCardCountByUser(100, 0)).willReturn(batch);

        doThrow(new RuntimeException("Kafka 연결 실패"))
                .when(reviewDueEventPort).publish(eq("user-1"), anyInt(), anyString());

        // when
        scheduler.publishDueEvents();

        // then — user-2는 정상 발행
        verify(reviewDueEventPort, times(1)).publish(eq("user-2"), eq(3), anyString());
    }

    @Test
    @DisplayName("배치 크기만큼 결과가 있으면 다음 페이지를 조회한다")
    void publishDueEvents_whenFullBatch_fetchesNextPage() {
        // given — 1페이지: 100개, 2페이지: 빈 배열
        List<Object[]> fullBatch = Collections.nCopies(100, row("user-x", 1));
        given(flashCardJpaRepository.findDueCardCountByUser(100, 0)).willReturn(fullBatch);
        given(flashCardJpaRepository.findDueCardCountByUser(100, 100)).willReturn(Collections.emptyList());

        // when
        scheduler.publishDueEvents();

        // then
        verify(flashCardJpaRepository, times(1)).findDueCardCountByUser(100, 0);
        verify(flashCardJpaRepository, times(1)).findDueCardCountByUser(100, 100);
        verify(reviewDueEventPort, times(100)).publish(anyString(), anyInt(), anyString());
    }
}

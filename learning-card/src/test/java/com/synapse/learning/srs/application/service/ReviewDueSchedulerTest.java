package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.FlashCardPort;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewDueSchedulerTest {

    @Mock
    private FlashCardPort flashCardPort;

    @Mock
    private ReviewDueEventPort reviewDueEventPort;

    @InjectMocks
    private ReviewDueScheduler scheduler;

    private static final String TENANT_ID = UUID.randomUUID().toString();

    private Object[] row(String userId, String tenantId, int count) {
        return new Object[] { userId, tenantId, count };
    }

    @Test
    @DisplayName("복습 대상 사용자가 있으면 이벤트를 발행한다")
    void publishDueEvents_whenUsersExist_publishesEvents() {
        List<Object[]> batch = List.of(
                row("user-1", TENANT_ID, 5),
                row("user-2", TENANT_ID, 3));
        given(flashCardPort.findDueCardCountByUser(100, 0)).willReturn(batch);

        scheduler.publishDueEvents();

        verify(reviewDueEventPort, times(1)).publish(eq("user-1"), eq(TENANT_ID), eq(5), anyString());
        verify(reviewDueEventPort, times(1)).publish(eq("user-2"), eq(TENANT_ID), eq(3), anyString());
        verify(reviewDueEventPort, times(2)).publish(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("복습 대상 사용자가 없으면 이벤트를 발행하지 않는다")
    void publishDueEvents_whenNoUsers_doesNotPublish() {
        given(flashCardPort.findDueCardCountByUser(anyInt(), anyInt()))
                .willReturn(Collections.emptyList());

        scheduler.publishDueEvents();

        verify(reviewDueEventPort, never()).publish(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("이벤트 발행 중 예외가 발생해도 다른 사용자는 계속 처리한다")
    void publishDueEvents_whenOneUserFails_continuesOthers() {
        List<Object[]> batch = List.of(
                row("user-1", TENANT_ID, 5),
                row("user-2", TENANT_ID, 3));
        given(flashCardPort.findDueCardCountByUser(100, 0)).willReturn(batch);

        doThrow(new RuntimeException("Kafka 연결 실패"))
                .when(reviewDueEventPort).publish(eq("user-1"), anyString(), anyInt(), anyString());

        scheduler.publishDueEvents();

        verify(reviewDueEventPort, times(1)).publish(eq("user-2"), eq(TENANT_ID), eq(3), anyString());
    }

    @Test
    @DisplayName("배치 크기만큼 결과가 있으면 다음 페이지를 조회한다")
    void publishDueEvents_whenFullBatch_fetchesNextPage() {
        List<Object[]> fullBatch = Collections.nCopies(100, row("user-x", TENANT_ID, 1));
        given(flashCardPort.findDueCardCountByUser(100, 0)).willReturn(fullBatch);
        given(flashCardPort.findDueCardCountByUser(100, 100)).willReturn(Collections.emptyList());

        scheduler.publishDueEvents();

        verify(flashCardPort, times(1)).findDueCardCountByUser(100, 0);
        verify(flashCardPort, times(1)).findDueCardCountByUser(100, 100);
        verify(reviewDueEventPort, times(100)).publish(anyString(), anyString(), anyInt(), anyString());
    }
}

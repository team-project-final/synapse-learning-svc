package com.synapse.learning.srs.application.service;

import com.synapse.learning.card.application.port.out.FlashCardPort;
import com.synapse.learning.srs.application.port.out.ReviewDueEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewDueScheduler {

    private static final int BATCH_SIZE = 100;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FlashCardPort flashCardPort;
    private final ReviewDueEventPort reviewDueEventPort;

    /**
     * 매일 08:00 KST — 복습 대상 사용자에게 card.review.due 이벤트 발행
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "ReviewDueScheduler_publishDueEvents", lockAtLeastFor = "1m", lockAtMostFor = "30m")
    public void publishDueEvents() {
        String dueDate = LocalDate.now(KST).toString();
        log.info("[ReviewDueScheduler] 복습 리마인더 이벤트 발행 시작 — dueDate={}", dueDate);

        int offset = 0;
        int totalPublished = 0;

        while (true) {
            List<Object[]> batch = flashCardPort
                    .findDueCardCountByUser(BATCH_SIZE, offset);

            if (batch.isEmpty())
                break;

            for (Object[] row : batch) {
                String userId = row[0].toString();
                String tenantId = row[1].toString();
                int dueCardCount = ((Number) row[2]).intValue();

                try {
                    reviewDueEventPort.publish(userId, tenantId, dueCardCount, dueDate);
                    totalPublished++;
                } catch (Exception e) {
                    log.error("[ReviewDueScheduler] 이벤트 발행 실패 — userId={}, error={}",
                            userId, e.getMessage(), e);
                }
            }

            if (batch.size() < BATCH_SIZE)
                break;
            offset += BATCH_SIZE;
        }

        log.info("[ReviewDueScheduler] 완료 — 총 {}명 이벤트 발행", totalPublished);
    }
}

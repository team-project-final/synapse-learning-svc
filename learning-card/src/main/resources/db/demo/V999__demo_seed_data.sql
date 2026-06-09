-- ============================================================
-- 발표용 데모 시드 데이터 (demo 프로파일 전용)
-- 실행 조건: spring.profiles.active=demo
-- ============================================================

-- 고정 UUID 상수
-- DEMO_TENANT_ID : a0000000-0000-0000-0000-000000000001
-- DEMO_USER_ID   : d0000000-0000-0000-0000-000000000001
-- DEMO_DECK_ID   : e0000000-0000-0000-0000-000000000001

-- ── 1. 덱 ────────────────────────────────────────────────────
INSERT INTO card_decks (id, tenant_id, user_id, name, description, color, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'Java & Spring 핵심 개념',
    '발표 데모용 덱 — JVM, Spring, Kafka, SRS 개념 카드 5장',
    '#4A90E2',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
);

-- ── 2. 카드 5장 ──────────────────────────────────────────────

-- 카드 1: JVM (review 상태 — 여러 번 복습 완료, 다음 복습 7일 후)
INSERT INTO cards (id, tenant_id, deck_id, card_type, front_content, back_content,
                   status, easiness_factor, interval_days, repetitions, lapses,
                   due_date, last_reviewed_at, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'qa',
    'JVM(Java Virtual Machine)이란 무엇인가요?',
    'Java 바이트코드를 실행하는 가상 머신입니다. 클래스 로더, 실행 엔진, 메모리 영역(Heap, Stack, Method Area)으로 구성되며 OS에 독립적인 실행 환경을 제공합니다.',
    'review',
    2.60,
    7,
    3,
    0,
    NOW() + INTERVAL '7 days',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day'
);

-- 카드 2: Spring IoC (review 상태 — 복습 완료, 다음 복습 4일 후)
INSERT INTO cards (id, tenant_id, deck_id, card_type, front_content, back_content,
                   status, easiness_factor, interval_days, repetitions, lapses,
                   due_date, last_reviewed_at, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'qa',
    'Spring IoC Container의 역할은 무엇인가요?',
    '객체(Bean)의 생성·관리·의존성 주입을 담당합니다. 개발자가 직접 객체를 생성하는 대신 컨테이너가 제어권을 가지며 이를 제어의 역전(IoC)이라 합니다.',
    'review',
    2.30,
    4,
    2,
    1,
    NOW() + INTERVAL '4 days',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 day'
);

-- 카드 3: @Transactional (learning 상태 — 어제 첫 복습, 오늘 다시 복습 대상)
INSERT INTO cards (id, tenant_id, deck_id, card_type, front_content, back_content,
                   status, easiness_factor, interval_days, repetitions, lapses,
                   due_date, last_reviewed_at, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'qa',
    '@Transactional 어노테이션의 동작 원리는?',
    'Spring AOP 프록시를 통해 메서드 호출 전·후에 트랜잭션을 시작·커밋·롤백합니다. 같은 클래스 내부 호출 시에는 프록시를 거치지 않아 트랜잭션이 적용되지 않는 self-invocation 주의가 필요합니다.',
    'learning',
    2.50,
    1,
    1,
    0,
    NOW(),
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '1 day'
);

-- 카드 4: Kafka (new 상태 — 아직 복습 안함)
INSERT INTO cards (id, tenant_id, deck_id, card_type, front_content, back_content,
                   status, easiness_factor, interval_days, repetitions, lapses,
                   due_date, last_reviewed_at, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'qa',
    'Apache Kafka에서 Producer와 Consumer의 역할은?',
    'Producer는 메시지를 토픽에 발행하고, Consumer는 토픽을 구독해 메시지를 소비합니다. Consumer Group으로 병렬 처리가 가능하며 파티션 단위로 순서가 보장됩니다.',
    'new',
    2.50,
    0,
    0,
    0,
    NOW(),
    NULL,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- 카드 5: SM-2 (new 상태 — 아직 복습 안함)
INSERT INTO cards (id, tenant_id, deck_id, card_type, front_content, back_content,
                   status, easiness_factor, interval_days, repetitions, lapses,
                   due_date, last_reviewed_at, created_at, updated_at)
VALUES (
    'c0000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'definition',
    'SM-2 간격 반복 알고리즘이란?',
    '복습 난이도(1~4등급)에 따라 다음 복습 간격을 계산하는 알고리즘입니다. Easiness Factor(EF)가 2.5에서 시작해 1.3~최대값 사이로 조정되며, Good 이상은 간격을 늘리고 Again은 1일로 리셋합니다.',
    'new',
    2.50,
    0,
    0,
    0,
    NOW(),
    NULL,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- ── 3. 복습 세션 2개 ─────────────────────────────────────────

-- 세션 1: 2일 전 완료 (카드 1, 2 복습)
INSERT INTO review_sessions (id, tenant_id, user_id, deck_id, status,
                              started_at, completed_at, total_cards, reviewed_cards)
VALUES (
    's0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'completed',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days' + INTERVAL '12 minutes',
    2,
    2
);

-- 세션 2: 어제 완료 (카드 1, 2, 3 복습)
INSERT INTO review_sessions (id, tenant_id, user_id, deck_id, status,
                              started_at, completed_at, total_cards, reviewed_cards)
VALUES (
    's0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'completed',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day' + INTERVAL '18 minutes',
    3,
    3
);

-- ── 4. 복습 이력 ─────────────────────────────────────────────

-- 세션 1: 카드 1 (GOOD=3), 카드 2 (GOOD=3)
INSERT INTO card_reviews (id, tenant_id, card_id, session_id, rating,
                          prev_ease_factor, new_ease_factor, prev_interval, new_interval,
                          time_spent_ms, reviewed_at)
VALUES (
    'r0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    's0000000-0000-0000-0000-000000000001',
    3,
    2.50, 2.50, 1, 3,
    8500,
    NOW() - INTERVAL '2 days'
);

INSERT INTO card_reviews (id, tenant_id, card_id, session_id, rating,
                          prev_ease_factor, new_ease_factor, prev_interval, new_interval,
                          time_spent_ms, reviewed_at)
VALUES (
    'r0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    's0000000-0000-0000-0000-000000000001',
    3,
    2.50, 2.30, 1, 2,
    12000,
    NOW() - INTERVAL '2 days'
);

-- 세션 2: 카드 1 (EASY=4), 카드 2 (GOOD=3), 카드 3 (GOOD=3)
INSERT INTO card_reviews (id, tenant_id, card_id, session_id, rating,
                          prev_ease_factor, new_ease_factor, prev_interval, new_interval,
                          time_spent_ms, reviewed_at)
VALUES (
    'r0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    's0000000-0000-0000-0000-000000000002',
    4,
    2.50, 2.60, 3, 7,
    5200,
    NOW() - INTERVAL '1 day'
);

INSERT INTO card_reviews (id, tenant_id, card_id, session_id, rating,
                          prev_ease_factor, new_ease_factor, prev_interval, new_interval,
                          time_spent_ms, reviewed_at)
VALUES (
    'r0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000002',
    's0000000-0000-0000-0000-000000000002',
    3,
    2.30, 2.30, 2, 4,
    9800,
    NOW() - INTERVAL '1 day'
);

INSERT INTO card_reviews (id, tenant_id, card_id, session_id, rating,
                          prev_ease_factor, new_ease_factor, prev_interval, new_interval,
                          time_spent_ms, reviewed_at)
VALUES (
    'r0000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000003',
    's0000000-0000-0000-0000-000000000002',
    3,
    2.50, 2.50, 0, 1,
    15300,
    NOW() - INTERVAL '1 day'
);

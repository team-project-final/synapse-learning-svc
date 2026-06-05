# Work History: @learning-card

> **담당**: learning-card / 덱·카드·SM-2  
> **관련 문서**: [SCOPE](../scope/SCOPE_learning-card.md) | [TASK](../task/TASK_learning-card.md) | [WORKFLOW](../workflow/WORKFLOW_learning-card_W1.md)

---

## 진행 상태 대시보드

### W1 (2026-05-12 ~ 05-16)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 1 | learning-card 골격 생성 | ✅ Done | 2026-05-15 | 2026-05-15 | 앱 기동 1.965초 확인 |
| Step 2 | 덱/카드 CRUD | ✅ Done | 2026-05-15 | 2026-05-18 | 덱 5개 + 카드 5개 엔드포인트 모두 완료 |
| Step 3 | SM-2 알고리즘 기초 | ✅ Done | 2026-05-18 | 2026-05-18 | rating 1~4 전체 시나리오 Swagger 수동 검증 완료 |

**W1 진행률**: 3/3 Steps 완료 🎉

### W2 (2026-05-19 ~ 05-23)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 4 | 복습 세션 관리 | ✅ Done | 2026-05-19 | 2026-05-19 | 4개 엔드포인트 Swagger 수동 검증 완료 |
| Step 5 | Kafka 이벤트 연동 | ✅ Done | 2026-05-20 | 2026-05-20 | card.reviewed Avro 이벤트 발행, EmbeddedKafka 테스트 완료 |
| Step 6 | 학습 통계 API | ✅ Done | 2026-05-21 | 2026-05-21 | GET /stats/overview + GET /stats/heatmap, 단위/컨트롤러 테스트 완료 |

**W2 진행률**: 3/3 Steps 완료 🎉

### W3 (2026-05-26 ~ 05-30)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 7 | review.due 스케줄러 | ✅ Done | 2026-05-26 | 2026-05-26 | card.review.due Avro 이벤트 + ShedLock 스케줄러 + 단위/통합 테스트 완료 |
| Step 8 | 복습 통계 대시보드 | ✅ Done | 2026-05-28 | 2026-05-28 | Redis 캐싱 TTL 5분 + StreakPort/MockStreakAdapter + 전체 테스트 JWT 적용 완료 |
| Step 9 | 복습 알림 | Not Started | — | — | |

**W3 진행률**: 2/3 Steps 완료

### W4 (2026-06-02 ~ 06-06)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 10 | E2E 테스트 | Not Started | — | — | |
| Step 11 | 안정화 | Not Started | — | — | |
| Step 12 | 문서화 | Not Started | — | — | |

**W4 진행률**: 0/3 Steps 완료

---

## 작업 로그

### W1 (2026-05-12 ~ 05-16)

#### 2026-05-12 (월)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-13 (화)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-14 (수)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-15 (목)
- **완료**: W1 Step1 골격 세팅 (Spring Boot 4 + Modulith, card/srs 모듈 구조, 앱 기동 확인)
- **진행 중**: W1 Step2-2 덱(Deck) CRUD 시작 준비
- **이슈**: sandbox 인터넷 불가 → 로컬 Cursor에서 빌드. PostgreSQL 18 신규 설치 완료
- **다음**: CardDeck Entity, Repository, Service, Controller, Flyway SQL

#### 2026-05-16 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

### W2 (2026-05-19 ~ 05-23)

#### 2026-05-18 (월)
- **완료**: W1 Step2 전체 완료 — 덱 CRUD 5개 + 카드 CRUD 5개 엔드포인트 구현 및 Swagger 테스트 완료
- **완료**: W1 Step3 전체 완료 — SM-2 알고리즘 구현 및 Swagger 검증 완료
  - 생성 파일: Sm2Result, Sm2Calculator, CardReview, CardReviewRepository, ReviewSubmitRequest, ReviewSubmitResponse, ReviewService, ReviewController, V10__init_card_reviews.sql
  - rating 1(Again)/2(Hard)/3(Good)/4(Easy) 4가지 시나리오 순서대로 검증 ✅
  - JSONB 파싱 이슈(regex→Jackson ObjectMapper) 해결
  - Spring Modulith OPEN module 설정으로 모듈 구조 테스트 통과
- **완료**: Step3 아키텍처 문서 기준 전면 정렬 완료
  - SM-2 공식 수정: quality 매핑 제거 → (4-rating) 직접 공식, AGAIN=EF-0.2, interval=0(10분)
  - cards 테이블: srs_state JSONB → 개별 SRS 컬럼 (easiness_factor, interval_days, repetitions, lapses, due_date)
  - front/back → front_content/back_content, state → status 컬럼 이름 정렬
  - card_reviews: id BIGSERIAL→UUID, repetitions 제거, time_spent_ms 추가
  - tenant_id: TEXT → UUID (card_decks, cards, card_reviews 전 테이블)
  - BaseEntity tenantId String → UUID, 전체 서비스 레이어 연동
  - Flyway V11~V13 마이그레이션 추가
  - Swagger 최종 검증: rating=3, newEaseFactor=2.5, intervalDays=1, lapses=0, dueDate=내일 ✅
- **이슈**: UUID vs TEXT 타입 불일치 → ALTER TABLE / updatedAt 미반영 → saveAndFlush() / bloomLevel 누락 컴파일 오류 → CardCreateRequest 수정 / JSONB regex 파싱 실패 → ObjectMapper 교체 / tenant_id USING 변환 실패(기존 "tenant-001" 값) → TRUNCATE 후 재실행
- **다음**: Step 3 PR 커밋 → push → GitHub PR → dev 머지 → Step 4 복습 세션 관리

#### 2026-05-19 (월)
- **완료**: W2 Step4 전체 완료 — 복습 세션 관리 4개 엔드포인트 구현 및 Swagger 검증 완료
  - 생성 파일: ReviewSession Entity, ReviewSessionRepository, ReviewSessionStartRequest, ReviewSessionResponse, ReviewCardResponse, ReviewSessionSubmitRequest, ReviewSessionService, ReviewSessionController
  - 생성 SQL: V14__init_review_sessions.sql, V15__add_session_id_to_reviews.sql
  - 수정 파일: FlashCardRepository (findDueCards 쿼리), CardReview (sessionId 필드), ReviewService (sessionId 파라미터 추가)
  - 삭제: ReviewController (세션 기반으로 대체)
  - API 검증: POST /reviews/sessions ✅ / GET /reviews/queue ✅ / POST /reviews/sessions/{id}/submit ✅ / PUT /reviews/sessions/{id}/complete ✅
- **이슈**: UUID string too large — Swagger X-Tenant-Id 헤더 잘못된 복사 → .trim() 방어 처리 추가
- **완료**: Step2~4 전체 단위 테스트 작성 완료
  - Sm2CalculatorTest (11개), ReviewServiceTest (3개), ReviewSessionServiceTest (5개)
  - ReviewSessionControllerTest (3개), CardServiceTest (6개), DeckServiceTest (6개)
  - 전체 34개 테스트 통과 ✅
- **다음**: Step 5 Kafka 이벤트 연동

#### 2026-05-20 (화)
- **완료**: W2 Step5 전체 완료 — card.reviewed Kafka Avro 이벤트 발행 구현
  - 생성 파일: card_reviewed.avsc (Avro 스키마), CardReviewedEventPublisher (srs/adapter/out/event)
  - 수정 파일: build.gradle.kts (Avro 플러그인 + Confluent 의존성), application.properties (Kafka Producer 설정), ReviewService (이벤트 발행 연동), card/package-info.java (OPEN 모듈 선언)
  - 테스트: CardReviewedEventPublisherTest (Mockito, 3개) + CardReviewedEventPublisherIntegrationTest (@EmbeddedKafka, 1개)
  - 파티션 키 = userId (같은 사용자 이벤트 순서 보장), 비동기 whenComplete 실패 로깅
- **이슈**: Spring Modulith MODULITH_TYPE_REF_VIOLATION — card 모듈이 CLOSED라 srs에서 FlashCard 참조 불가 → card/package-info.java OPEN 으로 수정
- **다음**: Step 6 학습 통계 API

#### 2026-05-21 (수)
- **완료**: W2 Step6 전체 완료 — 복습 통계 API 구현
  - 생성 파일: DailyReviewStatResponse, WeeklyReviewStatResponse, ReviewStatsResponse, WeeklyStatsResponse (DTO 4개)
  - 생성 파일: ReviewStatsUseCase (port/in), ReviewStatsPort (port/out), ReviewStatsService
  - 생성 파일: DailyStatRow, WeeklyStatRow (JPA Projection), ReviewStatsJpaRepository (Native Query), ReviewStatsPersistenceAdapter
  - 생성 파일: ReviewStatsController (GET /stats/overview, GET /stats/heatmap)
  - 테스트: ReviewStatsServiceTest (Mockito, 5개) + ReviewStatsControllerTest (3개) 통과 ✅
  - Native Query: DATE_TRUNC + FILTER(WHERE) + AT TIME ZONE 'Asia/Seoul' + JOIN review_sessions
  - 빈 날짜/주 0 채우기 로직 Java에서 처리
- **이슈**: —
- **다음**: Step 6 PR → dev 머지 → W2 완료 🎉

#### 2026-05-22 (목)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-23 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

### W3 (2026-05-26 ~ 05-30)

#### 2026-05-26 (월)
- **완료**: W3 Step7 전체 완료 — card.review.due 스케줄러 + Kafka 이벤트 발행 구현
  - 생성 파일: card_review_due.avsc (Avro 스키마), ReviewDueEventPort (port/out), ReviewDueEventPublisher (srs/adapter/out/event)
  - 생성 파일: SchedulerConfig (@EnableScheduling + ShedLock LockProvider), ReviewDueScheduler (@Scheduled 08:00 KST + ShedLock)
  - 생성 파일: V16__init_shedlock.sql (shedlock 테이블), ReviewDueSchedulerTest (단위 4개), ReviewDueEventPublisherIntegrationTest (EmbeddedKafka)
  - 수정 파일: build.gradle.kts (shedlock-spring:7.7.0 + shedlock-provider-jdbc-template:7.7.0), KafkaConfig (reviewDueProducerFactory + reviewDueKafkaTemplate), FlashCardJpaRepository (findDueCardCountByUser 네이티브 쿼리)
  - 배치 크기 100, offset 페이지네이션, 사용자별 예외 격리, 멱등성 Producer
- **이슈**: ShedLock 6.0.0 → 7.7.0 버전 수정 (Spring Boot 4 호환), Avro 코드 생성 미실행 → generateAvroJava 태스크 실행으로 해결
- **다음**: Step 8 복습 통계 대시보드 API

#### 2026-05-27 (화)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-28 (수)
- **완료**: Step 8 — 복습 통계 대시보드 Redis 캐싱 + 스트릭 Port/Adapter + 전체 테스트 JWT 적용
- **진행 중**: -
- **이슈**: 팀장 PR #25 SecurityConfig 추가로 기존 컨트롤러 테스트 전체 실패 → `@ActiveProfiles("test")` + `springSecurity()` + `.with(jwt())` 일괄 적용으로 해결. CacheConfig RedisConnectionFactory 요구로 통합 테스트도 `@ActiveProfiles("test")` 추가 필요
- **다음**: Step 9 — 복습 전체 E2E 테스트

#### 2026-05-29 (목)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-30 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

### W4 (2026-06-02 ~ 06-06)

#### 2026-06-02 (월)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-03 (화)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-04 (수)
- **완료**: WS3-A — Kafka security.protocol 배선 (코드리뷰 반영)
  - `KafkaConfig.java`: `@Value("${spring.kafka.security.protocol:PLAINTEXT}") securityProtocol` 필드 추가
  - `reviewCompletedProducerFactory`, `reviewDueProducerFactory` — PLAINTEXT가 아닐 때 `security.protocol` 조건부 적용
  - `ConsumerFactory<String, Object>` bean 신설 (Spring Boot auto-config 대체, security.protocol 동일 적용)
  - `application.yml`: `spring.kafka.security.protocol: ${SPRING_KAFKA_SECURITY_PROTOCOL:PLAINTEXT}` 추가
  - TDD: `KafkaConfigSecurityProtocolTest` 신설 (RED→GREEN) — SSL 주입/미주입 2케이스
  - 전체 테스트 BUILD SUCCESSFUL (회귀 없음)
  - 브랜치: `feat/kafka-learning-card-security-protocol`
- **진행 중**:
- **이슈**: MSK TLS-only(9094) 환경에서 security.protocol 미설정으로 SSL env 무시되던 문제 수정
- **다음**: PR → dev 머지

#### 2026-06-05 (목)
- **완료**: WS3-C — KAFKA_ENABLED 게이트 추가 (engagement 패턴 정합, PR #53)
  - `application.yml`: `synapse.kafka.enabled: ${KAFKA_ENABLED:false}` 바인딩
  - `KafkaConfig` / `CardReviewedEventPublisher` / `ReviewDueEventPublisher`: `@ConditionalOnProperty(enabled=true)` 추가
  - `NoopCardReviewedEventPublisher` / `NoopReviewDueEventPublisher` 신설 (matchIfMissing=true)
- **완료**: Step10 — Kafka DLQ 안정화
  - `KafkaDlqPort` 인터페이스 신설, `KafkaDlqPublisher` / `NoopKafkaDlqPublisher` 신설
  - `KafkaConfig`: DLQ용 `KafkaTemplate<String, String>` Bean 추가
  - `CardReviewedEventPublisher` / `ReviewDueEventPublisher`: 발행 실패 시 DLQ(`learning.card.dlq`) 저장 연동
  - `KafkaDlqPublisherTest` / `KafkaEnabledGateTest` 신설
  - 전체 테스트 BUILD SUCCESSFUL (74개)
  - 브랜치: `feat/LEARN-CARD-011-kafka-event-stabilization`
- **진행 중**: -
- **이슈**: WS3-C Noop 파일 커밋 누락 → Step10 브랜치에서 함께 포함
- **다음**: PR → dev 머지 → Step11(안정화) / Step12(문서화)

#### 2026-06-06 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

---

## 변경 이력

| 날짜 | 변경 사항 |
|------|-----------|
| 2026-05-11 | W2/W3/W4 대시보드 및 로그 템플릿 추가 |
| 2026-05-28 | Step8 Done — Redis 캐싱 TTL 5분 (CacheConfig + @Cacheable), StreakPort/MockStreakAdapter(헥사고날 fallback), 전체 컨트롤러 테스트 JWT 적용 (@ActiveProfiles("test") + springSecurity + jwt()) W3 Step8 완료 🎉 |
| 2026-05-26 | Step7 Done — card.review.due 스케줄러 + Kafka 이벤트 발행 구현 (Avro 스키마, ReviewDueEventPublisher, ShedLock 분산락, 배치 100 페이징, 단위/통합 테스트) W3 Step7 완료 🎉 |
| 2026-05-21 | Step6 Done — 복습 통계 API 구현 (GET /stats/overview 일별 30일, GET /stats/heatmap 주별 12주, Native Query DATE_TRUNC+FILTER, 빈날짜 Java 채우기) W2 전체 완료 🎉 |
| 2026-05-20 | Step5 Done — card.reviewed Avro 이벤트 발행 구현 (card_reviewed.avsc, CardReviewedEventPublisher, EmbeddedKafka 통합 테스트), Spring Modulith OPEN 모듈 설정 |
| 2026-05-19 | 단위 테스트 완료 — Step2~4 전체 34개 테스트 작성 및 통과 (Sm2Calculator, ReviewService, ReviewSessionService, ReviewSessionController, CardService, DeckService) |
| 2026-05-19 | Step4 Done — 복습 세션 관리 구�
# WORKFLOW: @learning-card-owner — Week 2

> **Task 문서**: [TASK_learning-card.md](../task/TASK_learning-card.md)
> **기간**: 2026-05-18 ~ 2026-05-22, 5 영업일
> **PRD**: [PRD_W2.md](../prd/PRD_W2.md)

---

## Step 4: SRS 복습 세션 완성 — 오늘 복습 카드 큐 + rating → SM-2 → 다음 복습일

### 4.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W2 해당 요구사항 확인 (SRS 복습 세션)
- [x] Duration 산정 확인

### 4.2 요구사항 분석
- [x] 오늘 복습 대상 카드 큐 조회 로직 정의 (due_date <= today)
- [x] 복습 세션 생성/완료 플로우 정의
- [x] rating 제출 → SM-2 → 다음 복습일 계산 플로우 확인
- [x] 세션 내 카드 순서 정책 (due_date ASC 오래된 순)
- [x] Instructions 초안 → TASK 문서 반영

### 4.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자 (본인 카드만 복습)
- [x] 공개 API 여부: No
- [x] 결과 → TASK Constraints 반영

### 4.4 ERD 설계
- [x] review_sessions 테이블 설계 (id UUID, tenant_id, user_id, deck_id, status: in_progress|completed|abandoned, started_at, completed_at, total_cards, reviewed_cards)
- [x] cards 테이블 갱신 확인 (due_date, interval_days, easiness_factor 컬럼)
- [x] 인덱스 설계 (tenant_id+user_id+started_at DESC, deck_id+status)
- [x] 관계 정의 (review_sessions.deck_id → card_decks.id FK)
- [x] Duration(final) 갱신

### 4.5 Security 2차 검토
- [x] 세션 데이터 접근 제어 (본인 세션만 — tenantId 기반)
- [x] Soft Delete 정책: 물리삭제 없음 (세션 이력 보관)
- [x] 행 단위 접근 제어: 필요 (tenantId 기반)
- [x] 결과 → TASK Constraints 반영

### 4.6 DTO / Entity 설계 (API First)
- [x] ReviewSessionResponse 정의 (sessionId, deckId, status, totalCards, reviewedCards, startedAt, completedAt)
- [x] ReviewCardResponse 정의 (cardId, cardType, frontContent, backContent, bloomLevel, repetitions, easinessFactor, dueDate)
- [x] ReviewSessionSubmitRequest 정의 (cardId, rating, timeSpentMs)
- [x] ReviewSessionStartRequest 정의 (deckId)
- [x] ReviewSession Entity 작성
- [x] MapStruct 매퍼 작성 — N/A (직접 매핑)
- [x] Output Format → TASK 반영

### 4.7 Repository 구현
- [x] ReviewSessionRepository 인터페이스 작성
- [x] findByIdAndTenantId 쿼리
- [x] findDueCards 쿼리 (due_date <= now AND status IN ('new','learning','review','relearning'), max 50)
- [x] Flyway 마이그레이션 스크립트 작성 (V14__init_review_sessions.sql, V15__add_session_id_to_reviews.sql)

### 4.8 Service + Test
- [x] ReviewSessionService 구현 (startSession, getReviewQueue, submitReview, completeSession)
- [x] 오늘 복습 카드 큐 조회 서비스 (due cards, max 50)
- [x] 세션 시작 로직 (totalCards 스냅샷)
- [x] rating 제출 → SM-2 호출 → due_date 갱신 → reviewedCards 증가
- [x] 세션 완료 로직 (completedAt + status: completed 갱신)
- [x] Bean Validation 적용
- [x] 단위 테스트 작성 (Mockito) — ReviewSessionServiceTest 작성 및 통과 ✅ (2026-05-20)
- [x] 통합 테스트 (세션 시작 → 복습 → 완료 플로우 검증)
- [x] 테스트 통과 확인 (`./gradlew.bat test` BUILD SUCCESSFUL ✅ 2026-05-20)

### 4.9 Controller + Test
- [x] POST /reviews/sessions 엔드포인트 구현 (세션 시작)
- [x] GET /reviews/queue 엔드포인트 구현 (카드 큐 — 별도 최상위 엔드포인트)
- [x] POST /reviews/sessions/{sessionId}/submit 엔드포인트 구현 (rating 제출)
- [x] PUT /reviews/sessions/{sessionId}/complete 엔드포인트 구현 (세션 완료)
- [x] 슬라이스 테스트 — @SpringBootTest(MOCK) + webAppContextSetup 방식으로 ReviewSessionControllerTest 작성 ✅ (2026-05-20)
- [x] 403 응답 테스트 — submitReview 접근 불가 시나리오 검증 완료 / 401은 API Gateway 영역 (JWT 검증 후 X-User-Id 헤더 주입) — 이 서비스 범위 아님
- [x] 통합 테스트 (전체 복습 플로우 검증)
- [x] 테스트 통과 확인 (`./gradlew.bat test` BUILD SUCCESSFUL ✅ 2026-05-20)

### 4.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] Swagger API 문서 확인 (4개 엔드포인트 전체 동작 확인)
- [x] RULE Reference → TASK 반영

**Step 4 Status**: [ ] Not Started / [ ] In Progress / [x] Done (2026-05-20)

---

## Step 5: card.reviewed Kafka 발행 — Avro 스키마 + 이벤트 발행

### 5.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W2 해당 요구사항 확인 (card.reviewed Kafka 발행)
- [x] Duration 산정 확인

### 5.2 요구사항 분석
- [x] card.reviewed 이벤트 페이로드 정의 (userId, cardId, deckId, rating, reviewedAt)
- [x] Avro 스키마 필드 타입 정의
- [x] Schema Registry 등록 절차 확인
- [x] 이벤트 발행 시점 정의 (rating 제출 직후)
- [x] Instructions 초안 → TASK 문서 반영

### 5.3 Security 1차 검토
- [x] Kafka 프로듀서 인증 설정 확인
- [x] 메시지 무결성 보장 (Avro 직렬화)
- [x] 민감 정보 포함 여부 확인 (userId만, 개인정보 미포함)
- [x] 결과 → TASK Constraints 반영

### 5.4 ERD 설계
- [x] ERD 해당 없음 (이벤트 발행 — DB 변경 없음)
- [x] Avro 스키마 파일 설계 (.avsc)
- [x] Duration(final) 갱신

### 5.5 Security 2차 검토
- [x] Schema Registry BACKWARD 호환성 검증 확인
- [x] 메시지 암호화 전송 (TLS) 확인
- [x] 결과 → TASK Constraints 반영

### 5.6 DTO / Entity 설계 (API First)
- [x] CardReviewedEvent Avro 스키마 정의 (card_reviewed.avsc)
- [x] 필드 정의: userId(string), cardId(string), deckId(string), rating(enum), reviewedAt(timestamp-millis)
- [x] default 값 설정 (BACKWARD 호환성)
- [x] Output Format → TASK 반영

### 5.7 Repository 구현
- [x] Avro 스키마 파일 작성 (src/main/avro/card_reviewed.avsc)
- [x] Avro 코드 생성 플러그인 설정 (Gradle avro plugin)
- [ ] Schema Registry에 스키마 등록 (POST /subjects/card.reviewed-value/versions) — 로컬 실행 후

### 5.8 Service + Test
- [x] CardReviewedEventPublisher 구현 (KafkaTemplate + Avro 직렬화)
- [x] ReviewService에 이벤트 발행 로직 추가 (rating 제출 후, 트랜잭션 커밋 후 발행)
- [x] 이벤트 발행 실패 시 재시도/로깅 처리
- [x] 트랜잭션 아웃박스 패턴 검토 (선택) — TransactionSynchronization afterCommit + 비동기 whenComplete 로깅 적용
- [x] 단위 테스트 작성 (Mockito — KafkaTemplate mock) ✅ CardReviewedEventPublisherTest (3개)
- [x] Kafka 발행 테스트 (@EmbeddedKafka) ✅ CardReviewedEventPublisherIntegrationTest (1개)
- [x] 테스트 통과 확인 (`./gradlew.bat test` BUILD SUCCESSFUL ✅ 2026-05-20)

### 5.9 Controller + Test
- [x] Controller 해당 없음 (내부 이벤트 발행)
- [x] 통합 테스트 (rating 제출 → Kafka 메시지 발행 확인) ✅ EmbeddedKafka로 검증
- [x] Schema Registry 호환성 테스트 (비호환 스키마 → 거부) — mock://test-scope로 검증
- [x] 테스트 통과 확인 (`./gradlew.bat test` BUILD SUCCESSFUL ✅ 2026-05-20)

### 5.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음
- [ ] Kafka 토픽 메시지 확인 (kafka-console-consumer) — 로컬 실행 후
- [x] RULE Reference → TASK 반영

**Step 5 Status**: [ ] Not Started / [ ] In Progress / [x] Done (2026-05-20)

---

## Step 6: review_sessions 통계 — 일별/주별 복습 수, 정답률 API

### 6.1 TASK 시작
- [ ] Step Goal / Done When / Scope / Input 확인
- [ ] PRD_W2 해당 요구사항 확인 (복습 통계)
- [ ] Duration 산정 확인

### 6.2 요구사항 분석
- [ ] 일별 복습 수 집계 요건 (최근 N일)
- [ ] 주별 복습 수 집계 요건 (최근 N주)
- [ ] 정답률 계산 기준 정의 (Good+Easy / 전체)
- [ ] 통계 데이터 응답 포맷 정의
- [ ] Instructions 초안 → TASK 문서 반영

### 6.3 Security 1차 검토
- [ ] 인증 필요 여부: Yes (JWT 인증 필요)
- [ ] 권한 종류: 로그인 사용자 (본인 통계만)
- [ ] 공개 API 여부: No
- [ ] 결과 → TASK Constraints 반영

### 6.4 ERD 설계
- [ ] 추가 테이블 불필요 (card_reviews + review_sessions 기반 집계)
- [ ] 집계 쿼리 설계 (GROUP BY date/week, COUNT, 정답률 계산)
- [ ] 인덱스 최적화 확인 (card_reviews.user_id + reviewed_at)
- [ ] Duration(final) 갱신

### 6.5 Security 2차 검토
- [ ] 통계 데이터 접근 제어 (본인만)
- [ ] 다른 사용자 통계 접근 불가 확인
- [ ] 결과 → TASK Constraints 반영

### 6.6 DTO / Entity 설계 (API First)
- [ ] DailyReviewStatResponse 정의 (date, reviewCount, correctRate)
- [ ] WeeklyReviewStatResponse 정의 (weekStart, reviewCount, correctRate)
- [ ] ReviewStatsResponse 정의 (daily[], weekly[], totalReviews, overallCorrectRate)
- [ ] Output Format → TASK 반영

### 6.7 Repository 구현
- [ ] CardReviewRepository 커스텀 쿼리 추가
- [ ] 일별 집계 쿼리 (GROUP BY DATE(reviewed_at), COUNT, 정답률)
- [ ] 주별 집계 쿼리 (GROUP BY WEEK, COUNT, 정답률)
- [ ] Native Query 또는 JPQL 작성

### 6.8 Service + Test
- [ ] ReviewStatsService 구현 (getDailyStats, getWeeklyStats, getOverallStats)
- [ ] 일별 복습 수 + 정답률 계산 로직
- [ ] 주별 복습 수 + 정답률 계산 로직
- [ ] 기간 필터링 (최근 30일 / 12주)
- [ ] 데이터 없는 날짜 0으로 채우기 로직
- [ ] 단위 테스트 작성 (Mockito)
- [ ] 경계값 테스트 (데이터 없는 기간, 정답률 0%)
- [ ] 테스트 통과 확인

### 6.9 Controller + Test
- [ ] GET /stats/overview 엔드포인트 구현 (일별 통계 + 종합)
- [ ] GET /stats/heatmap 엔드포인트 구현 (주별 통계)
- [ ] 슬라이스 테스트 (@WebMvcTest)
- [ ] 401/403 응답 테스트
- [ ] 통합 테스트 (복습 데이터 → 통계 API 검증)
- [ ] 테스트 통과 확인

### 6.10 View + Test (해당 시)
- [ ] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [ ] Swagger API 문서 확인
- [ ] RULE Reference → TASK 반영

**Step 6 Status**: [ ] Not Started / [ ] In Progress / [ ] Done

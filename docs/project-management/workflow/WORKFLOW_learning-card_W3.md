# WORKFLOW: @learning-card-owner — Week 3

> **Task 문서**: [TASK_learning-card.md](../task/TASK_learning-card.md)  
> **기간**: 2026-05-26 ~ 2026-05-30  
> **PRD**: [PRD_W3.md](../prd/PRD_W3.md)

---

## Step 7: card.review.due 발행 — 매일 스케줄러 → 복습 대상 사용자 → Kafka 발행

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W3 해당 요구사항 확인 (복습 알림 Kafka 발행)
- [x] Duration 산정 확인

### 1.2 요구사항 분석
- [x] 스케줄러 실행 시간 정의 (매일 오전 8시 KST)
- [x] 복습 대상 판정 기준 분석 (SM-2 알고리즘 due_date ≤ today)
- [x] card.review.due 이벤트 스키마 정의 (userId, cardCount, dueDate)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: No (내부 스케줄러, API 미노출)
- [x] Kafka 토픽 ACL: learning-card-svc만 발행 권한
- [x] 스케줄러 중복 실행 방지 (ShedLock 등)
- [x] 결과 → TASK Constraints 반영

### 1.4 Kafka 토픽 설계
- [x] card.review.due 토픽 설정 (파티션, 복제, 보존)
- [x] 이벤트 키 전략 (userId 기반 파티셔닝)
- [x] Schema Registry 스키마 등록
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토
- [x] 이벤트 페이로드 민감정보 미포함 확인 (카드 내용 X, 건수만)
- [x] 스케줄러 실패 시 재시도 정책 (최대 3회)
- [x] 대량 사용자 처리 시 배치 크기 제한
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)
- [x] CardReviewDueEvent DTO 정의 (userId, cardCount, dueDate, occurredAt)
- [x] Avro/JSON Schema 작성 → Schema Registry 등록
- [x] Output Format → TASK 반영

### 1.7 Producer 구현
- [x] ReviewDueKafkaProducer 구현 (KafkaTemplate)
- [x] publishReviewDue(userId, cardCount) 메서드 구현
- [x] 멱등성 Producer 설정 (enable.idempotence=true)
- [x] 배치 발행 최적화 (사용자별 개별 이벤트)

### 1.8 Service + Test
- [x] ReviewDueScheduler 구현 (@Scheduled cron = "0 0 8 * * *" Asia/Seoul)
- [x] ShedLock 설정 (중복 실행 방지, lockAtMostFor=30m)
- [x] 복습 대상 사용자 조회 쿼리 (due_date ≤ today, GROUP BY user_id)
- [x] 사용자별 card.review.due 이벤트 발행
- [x] 통합 테스트 작성 (EmbeddedKafka + 스케줄러 트리거)
- [x] 테스트 통과 확인

### 1.9 E2E 검증
- [x] Docker Compose 환경에서 스케줄러 수동 트리거
- [x] kafka-console-consumer로 card.review.due 이벤트 수신 확인 (팀 Kafka 인프라 세팅 후 진행)
- [ ] notification 서비스 연동 테스트 (이벤트 → 알림 발송)
- [ ] 대량 사용자 시뮬레이션 (1000명 기준 처리 시간 측정)

### 1.10 결과 정리
- [x] 이벤트 스키마 문서화 (card_review_due.avsc)
- [x] 스케줄러 모니터링 방안 (실행 로그, 실패 알림 — 로그 기반)
- [x] RULE Reference → TASK 반영

**Step 7 Status**: [ ] Not Started / [ ] In Progress / [x] Done

---

## Step 8: 복습 통계 대시보드 — 일별/주별 복습 수, 정답률, 스트릭 통합 API

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W3 해당 요구사항 확인 (복습 통계 대시보드)
- [x] Duration 산정 확인

### 1.2 요구사항 분석
- [x] 일별 복습 통계 요건 (복습 수, 정답률)
- [x] 주별 복습 통계 요건 (주간 합산, 추세 그래프 데이터)
- [x] 스트릭 통합 요건 (engagement-svc 연동 — 스트릭 데이터는 `user_profiles_gamification` 테이블 기반이며 Kafka 이벤트를 통해 수신; engagement-svc DB에 직접 접근하지 않음)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (로그인 사용자)
- [x] 권한 종류: 본인 통계만 조회 가능
- [x] 공개 API 여부: No
- [x] 결과 → TASK Constraints 반영

### 1.4 데이터 모델 설계
- [x] 일별 통계 집계 방식 결정 (실시간 집계 — card_reviews 테이블 GROUP BY)
- [x] review_daily_stats 테이블 설계 (별도 테이블 없음 — card_reviews 집계 쿼리 방식 채택)
- [x] 인덱스 설계 (user_id + date, 기존 card_reviews 인덱스 활용)
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토
- [x] 타인 통계 조회 차단 (user_id 필터 필수)
- [x] 통계 데이터 민감정보 미포함
- [x] API 응답 캐싱 (Redis TTL: 5분)
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)
- [x] ReviewDailyStat Entity 작성 (집계 쿼리 방식 — DailyStatRow projection)
- [x] DailyReviewStatResponse DTO 정의 (date, reviewCount, correctRate)
- [x] WeeklyReviewStatResponse DTO 정의 (weekStart, reviewCount, correctRate)
- [x] ReviewStatsResponse DTO 정의 (daily, totalReviews, overallCorrectRate, currentStreak, longestStreak)
- [x] Output Format → TASK 반영

### 1.7 Repository 구현
- [x] ReviewStatsJpaRepository 구현 (일별/주별 집계 쿼리 — 팀원 PR #20)
- [x] 일별 통계: DATE_TRUNC + FILTER COUNT
- [x] 주별 통계: 최근 12주 집계
- [x] engagement-svc 연동 → StreakPort 인터페이스 + MockStreakAdapter fallback (Kafka 미확정)

### 1.8 Service + Test
- [x] ReviewStatsService 구현 (일별 통계 조회 — 팀원 PR #20)
- [x] ReviewStatsService 구현 (주별 통계 조회 — 최근 12주)
- [x] StreakPort 주입 — MockStreakAdapter(currentStreak=0, longestStreak=0) fallback
- [x] 스트릭 데이터 연동 → MockStreakAdapter (engagement-svc Kafka 확정 시 KafkaStreakAdapter로 교체)
- [x] Redis 캐싱 적용 (TTL: 5분 — CacheConfig)
- [x] 단위 테스트 작성 (Mockito)
- [x] 테스트 통과 확인

### 1.9 Controller + Test
- [x] GET /stats/overview 엔드포인트 구현 (일별 30일 + 스트릭 통합)
- [x] GET /stats/heatmap 엔드포인트 구현 (주별 히트맵, 최근 12주)
- [x] ReviewStatsControllerTest — @ActiveProfiles("test") + springSecurity() + jwt()
- [x] 401(JWT 없음), 400(헤더 누락), 200(정상) 케이스 테스트
- [x] 테스트 통과 확인
- [x] 전체 컨트롤러 테스트 JWT 적용 (Card/Deck/ReviewSession/Integration 포함)

### 1.10 View + T
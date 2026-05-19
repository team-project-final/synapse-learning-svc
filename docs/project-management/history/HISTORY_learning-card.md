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
| Step 5 | Kafka 이벤트 연동 | Not Started | — | — | |
| Step 6 | 학습 통계 API | Not Started | — | — | |

**W2 진행률**: 1/3 Steps 완료

### W3 (2026-05-26 ~ 05-30)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 7 | review.due 스케줄러 | Not Started | — | — | |
| Step 8 | 학습 대시보드 API | Not Started | — | — | |
| Step 9 | 복습 알림 | Not Started | — | — | |

**W3 진행률**: 0/3 Steps 완료

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
- **완료**: Step3 아키텍처 문서 기준 전면 정렬 완료
  - SM-2 공식 수정: quality 매핑 제거 → (4-rating) 직접 공식, AGAIN=EF-0.2, interval=0(10분)
  - cards 테이블: srs_state JSONB → 개별 SRS 컬럼 (easiness_factor, interval_days, repetitions, lapses, due_date)
  - front/back → front_content/back_content, state → status 컬럼 이름 정렬
  - card_reviews: id BIGSERIAL→UUID, repetitions 제거, time_spent_ms 추가
  - tenant_id: TEXT → UUID (card_decks, cards, card_reviews 전 테이블)
  - BaseEntity tenantId String → UUID, 전체 서비스 레이어 연동
  - Flyway V11~V13 마이그레이션 추가
  - Swagger 최종 검증: rating=3, newEaseFactor=2.5, intervalDays=1, lapses=0, dueDate=내일 ✅
- **이슈**: UUID vs TEXT 타입 불일치 → ALTER TABLE / tenant_id USING 변환 실패(기존 "tenant-001" 값) → TRUNCATE 후 재실행
- **다음**: Step 4 복습 세션 관리

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
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-21 (수)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

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
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-27 (화)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-05-28 (수)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

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
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-05 (목)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-06 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

---

## 변경 이력

| 날짜 | 변경 사항 |
|------|-----------|
| 2026-05-19 | 단위 테스트 완료 — Step2~4 전체 34개 테스트 작성 및 통과 (Sm2Calculator, ReviewService, ReviewSessionService, ReviewSessionController, CardService, DeckService) |
| 2026-05-19 | Step4 Done — 복습 세션 관리 구현 (ReviewSession, ReviewSessionService, ReviewSessionController, V14~V15 SQL) 및 4개 엔드포인트 Swagger 검증 완료 |
| 2026-05-18 | Step3 아키텍처 정렬 완료 — cards JSONB→개별컬럼, card_reviews UUID+time_spent_ms, tenant_id UUID, SM-2 공식 수정, Flyway V11~V13 |
| 2026-05-18 | Step3 Done — SM-2 알고리즘 구현 (Sm2Calculator, ReviewService, ReviewController, V10 SQL) 및 rating 1~4 Swagger 검증 완료 |
| 2026-05-18 | Step2 Done — 덱 CRUD 5개 + 카드 CRUD 5개 API 구현 및 테스트 완료 |
| 2026-05-15 | Step1 Done — Spring Boot 4 + Modulith 골격 생성, 앱 기동 확인 |
| 2026-05-11 | W2/W3/W4 대시보드 및 로그 템플릿 추가 |
| 2026-05-11 | 초기 템플릿 생성 |

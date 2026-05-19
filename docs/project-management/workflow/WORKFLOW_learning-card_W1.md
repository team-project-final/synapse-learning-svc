# WORKFLOW: @learning-card-owner — Week 1

> **Task 문서**: [TASK_learning-card.md](../task/TASK_learning-card.md)
> **기간**: 2026-05-12 ~ 2026-05-15, 4 영업일
> **기능개발 Workflow**: [README §7](../README.md)

---

## Step 1: 프로젝트 초기 설정

### 1.1 TASK 시작

- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (프로젝트 골격)
- [x] Duration 산정 확인 (0.5일)

### 1.2 요구사항 분석

- [x] Spring Boot 4 + Modulith 프로젝트 구조 분석
- [x] card/srs 2개 모듈 역할 정의
- [x] 팀 공통 Gradle 설정 확인
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토

- [x] 인증 필요 여부: No (골격만 생성)
- [x] 권한 종류: 없음
- [x] 공개 API 여부: No (Health endpoint만)
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계

- [x] 골격 단계 — ERD 해당 없음
- [x] 모듈별 패키지 구조도 작성
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토

- [x] 민감 정보 암호화: 비해당 (골격 단계)
- [x] Soft Delete 정책: 비해당
- [x] 행 단위 접근 제어: 불필요
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)

- [x] 골격 단계 — 빈 Controller/Service 클래스만 생성
- [x] 각 모듈 package-info.java + @ApplicationModule 설정
- [x] Output Format → TASK 반영

### 1.7 Repository 구현

- [x] 골격 단계 — Repository 해당 없음
- [x] ApplicationModulesTest 구조 검증 테스트 작성

### 1.8 Service + Test

- [x] 빈 Service 클래스 생성 (card, srs 모듈)
- [x] ApplicationModulesTest 통과 확인
- [x] `./gradlew build` 성공 확인
- [x] Health endpoint (`/actuator/health`) 확인

### 1.9 Controller + Test

- [x] 빈 Controller 클래스 생성 (card, srs 모듈)
- [x] Dockerfile 작성 (multi-stage build)
- [x] Docker 이미지 빌드 성공 확인

### 1.10 View + Test (해당 시)

- [x] Flutter 화면 연동: 해당 없음
- [x] docker compose에서 learning-card 런타임 실행 확인 (actuator/health → UP ✅)
- [x] RULE Reference → TASK 반영

**Step 1 Status**: [x] Done (2026-05-15)

---

## Step 2: 덱/카드 CRUD API

### 1.1 TASK 시작

- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (FR-LC-xxx 덱/카드 CRUD)
- [x] Duration 산정 확인 (2일)

### 1.2 요구사항 분석

- [x] Deck/Card CRUD API 엔드포인트 정의
- [x] 1:N 관계 (Deck → Card) RESTful URL 규칙 확인
- [x] 페이지네이션 요건 (기본 20건, 최대 100건) — PageResponse<T> 도입, GET /decks + GET /decks/{id}/cards 적용 (2026-05-19)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토

- [x] 인증 필요 여부: Yes (JWT 인증 필요, userId는 JWT에서 추출)
- [x] 권한 종류: 로그인 사용자 (본인 덱/카드만)
- [x] 공개 API 여부: No
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계

- [x] card_decks 테이블 설계 (id, tenant_id, user_id, name, description, created_at, updated_at, deleted_at)
- [x] cards 테이블 설계 (id, deck_id, front_content, back_content, card_type: basic|cloze|reverse, status: new|learning|review|suspended, easiness_factor float DEFAULT 2.5, interval_days, repetitions, lapses, due_date, source_note_id, source_chunk_id, created_at, updated_at, deleted_at)
- [x] 인덱스 설계 (card_decks.user_id, cards.deck_id)
- [x] 관계 정의 (cards.deck_id → card_decks.id FK)
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토

- [x] 민감 정보 암호화: 비해당
- [x] Soft Delete 정책: 논리삭제 (deletedAt 컬럼)
- [x] 행 단위 접근 제어: 필요 (userId 기반 덱 소유 확인)
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)

- [x] DeckCreateRequest 정의 (name, description)
- [x] DeckResponse 정의 (id, name, description, cardCount, createdAt)
- [x] CardCreateRequest 정의 (front_content, back_content, card_type)
- [x] CardResponse 정의 (id, front_content, back_content, card_type, status, due_date, createdAt)
- [x] CardDeck Entity 작성
- [x] Card Entity 작성
- [x] MapStruct 매퍼 작성
- [x] Output Format → TASK 반영

### 1.7 Repository 구현

- [x] CardDeckRepository 인터페이스 작성
- [x] CardRepository 인터페이스 작성
- [x] findByUserIdAndDeletedAtIsNull 커스텀 쿼리
- [x] Flyway 마이그레이션 스크립트 작성

### 1.8 Service + Test

- [x] CardDeckService CRUD 구현 (create, findAll, findById, update, delete)
- [x] CardService CRUD 구현 (create, findByDeckId, update, delete)
- [x] 소유자 검증 로직 구현
- [x] Bean Validation 적용
- [x] 단위 테스트 작성 (Mockito) — CardServiceTest, DeckServiceTest 작성 완료 ✅
- [x] 테스트 통과 확인 (Swagger 수동 + 단위 테스트)

### 1.9 Controller + Test

- [x] CardDeckController REST API 구현 (POST/GET/PATCH/DELETE)
- [x] CardController REST API 구현 (`/decks/{deckId}/cards`)
- [x] 슬라이스 테스트 — @WebMvcTest 미지원(Spring Boot 4) → @SpringBootTest(MOCK) 방식으로 DeckControllerTest(5개), CardControllerTest(5개) 작성 (2026-05-19)
- [x] 403 응답 테스트 — 소유자 아님 시나리오 (DECK_ACCESS_DENIED) 검증 완료 / 401은 JWT 미구현으로 추후 진행
- [x] 통합 테스트 (각 엔드포인트별)
- [x] 테스트 통과 확인 (Swagger 수동)

### 1.10 View + Test (해당 시)

- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] Swagger API 문서 확인
- [x] RULE Reference → TASK 반영

**Step 2 Status**: [ ] Not Started / [ ] In Progress / [x] Done (2026-05-18)

---

## Step 3: SM-2 알고리즘 구현

### 1.1 TASK 시작

- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (FR-LC-xxx SM-2 복습)
- [x] Duration 산정 확인 (1.5일)

### 1.2 요구사항 분석

- [x] SM-2 알고리즘 논문/명세 분석
- [x] rating 값 정의: int 1-4 (1=Again, 2=Hard, 3=Good, 4=Easy)
- [x] interval/EF 계산 규칙 정리
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토

- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자 (본인 카드만 복습)
- [x] 공개 API 여부: No
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계

- [x] card_reviews 테이블 설계 (id, tenant_id, card_id, rating, prev/new_ease_factor, prev/new_interval, repetitions, reviewed_at)
- [x] 인덱스 설계 (card_id+reviewed_at DESC, tenant_id+reviewed_at DESC)
- [x] 관계 정의 (card_reviews.card_id → cards.id FK)
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토

- [x] 민감 정보 암호화: 비해당
- [x] Soft Delete 정책: 물리삭제 (로그 누적 보관)
- [x] 행 단위 접근 제어: 필요 (본인 카드 복습 기록만)
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)

- [x] ReviewSubmitRequest 정의 (rating int 1-4, @Min @Max validation)
- [x] ReviewSubmitResponse 정의 (cardId, rating, newEaseFactor, newIntervalDays, repetitions, nextReviewAt)
- [x] CardReview Entity 작성 (BIGSERIAL PK)
- [x] Rating 값 정의: int 1=Again, 2=Hard, 3=Good, 4=Easy
- [x] MapStruct 매퍼 작성 (직접 매핑으로 대체 — N/A)
- [x] Output Format → TASK 반영

### 1.7 Repository 구현

- [x] CardReviewRepository 인터페이스 작성
- [x] findTopByCardIdOrderByReviewedAtDesc 커스텀 쿼리
- [x] Flyway 마이그레이션 스크립트 작성 (V10__init_card_reviews.sql, pgAdmin 수동 실행)

### 1.8 Service + Test

- [x] Sm2Calculator 도메인 서비스 구현
- [x] interval 계산 로직 (Again/Hard→1, 첫성공→1, 두번째→6, 이후→interval*EF)
- [x] easeFactor 업데이트 로직 (최소 1.3 보장, 소수점 2자리 반올림)
- [x] 단위 테스트 작성 (4개 rating x 초기/중간/고EF 경계값) — Sm2CalculatorTest, ReviewServiceTest 작성 완료 ✅
- [x] 부동소수점 반올림 처리 확인
- [x] Swagger 수동 검증 통과 (rating 1~4 전체 시나리오)

### 1.9 Controller + Test

- [x] POST /cards/{cardId}/reviews 엔드포인트 구현 (ReviewController → ReviewSessionController로 대체)
- [x] 슬라이스 테스트 — @SpringBootTest(MOCK) 방식으로 ReviewSessionControllerTest에 submitReview(200) + 403 테스트 추가 (2026-05-19)
- [x] 403 응답 테스트 — 세션 접근 불가 시나리오 검증 완료 / 401은 JWT 미구현으로 추후 진행
- [x] 통합 테스트 (4개 rating 순서 테스트, EF/interval/rep 모두 정확)
- [x] 테스트 통과 확인 (Swagger 수동)

### 1.10 View + Test (해당 시)

- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] Swagger API 문서 확인
- [x] RULE Reference → TASK 반영

### 1.11 아키텍처 문서 기준 정렬 (추가 작업)

- [x] SM-2 공식 수정: quality 매핑 제거 → (4-rating) 직접 공식, AGAIN=EF-0.2, interval=0(10분 후)
- [x] cards 테이블: srs_state JSONB → 개별 SRS 컬럼 (easiness_factor, interval_days, repetitions, lapses, due_date)
- [x] front/back → front_content/back_content, state → status 컬럼 이름 정렬
- [x] card_reviews: id BIGSERIAL→UUID, repetitions 제거, time_spent_ms 추가
- [x] tenant_id: TEXT → UUID 전 테이블 적용 (V13 마이그레이션)
- [x] BaseEntity tenantId String → UUID, 서비스 레이어 전체 연동
- [x] ReviewService JSONB 파싱 전면 제거 → 직접 필드 접근으로 단순화
- [x] Flyway V11~V13 작성 및 pgAdmin 수동 실행 완료
- [x] Swagger 최종 검증: rating=3, newEaseFactor=2.5, intervalDays=1, lapses=0, dueDate=내일 ✅

**Step 3 Status**: [ ] Not Started / [ ] In Progress / [x] Done (2026-05-18)

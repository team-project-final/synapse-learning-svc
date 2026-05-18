# 작업 스코프: @learning-card-owner

## 담당자 정보

| 항목 | 내용 |
|------|------|
| Handle | @learning-card-owner |
| 역할 | 트랙 D-1 (2명 중 1명) |
| 담당 서비스 | synapse-learning-svc / learning-card |
| 담당 모듈 | card, srs (Java) |
| GitHub Repository | [synapse-learning-svc](https://github.com/team-project-final/synapse-learning-svc) (`learning-card` 모듈) |

## 5주 전체 책임 범위

### 도메인 경계

- **In Scope**:
  - 덱(Deck) CRUD (이름, 설명, 공개 여부)
  - 카드(Card) CRUD (앞면/뒷면, 태그, 미디어)
  - SM-2 알고리즘 (ease factor, interval, repetition 계산)
  - 복습 세션 (카드 큐 → rating → SM-2 → 다음 복습일)
  - card.reviewed Kafka 이벤트 발행 (XP 적립 트리거)
  - card.review.due 이벤트 발행 (복습 리마인더)
  - review_sessions 통계 (일별/주별 복습 카드 수, 정답률)
  - **카드 일괄 생성**: `POST /cards/batch` (Wiki에 정의되어 있으나 SCOPE에 누락) *(Wiki API 명세서 동기화 — 추가)*
  - **복습 큐 조회**: `GET /reviews/queue` (오늘의 복습 큐) *(Wiki API 명세서 동기화 — 추가)*
  - **세션 완료**: `PUT /reviews/sessions/{sessionId}/complete` *(Wiki API 명세서 동기화 — 추가)*
  - **Stats 히트맵/리텐션**: `GET /stats/heatmap` (GitHub 스타일 히트맵), `GET /stats/retention` (리텐션 커브 데이터) — 현재 overview만 암시되어 있음 *(Wiki API 명세서 동기화 — 추가)*
- **Out of Scope**:
  - AI 카드 자동 생성 (learning-ai-owner 담당)
  - 게이미피케이션 로직 (engagement 담당)
  - 알림 발송 (platform 담당)

### 주차별 스코프 매트릭스

| 주차 | 기간 | 핵심 목표 | 산출물 | 의존성 |
|------|------|-----------|--------|--------|
| W1 | 05-12~15 | learning-card 골격 + 덱/카드 CRUD + SM-2 기초 | 서비스 골격, 덱/카드 API, SM-2 유틸 | 인프라 (team-lead) |
| W2 | 05-18~22 | 복습 세션 완성 + card.reviewed Kafka 발행 + 카드 일괄 생성 + 복습 큐 조회 | 복습 세션 API, Kafka 발행, review_sessions, `POST /cards/batch`, `GET /reviews/queue` | Kafka 토픽 (team-lead W2) |
| W3 | 05-26~29 | card.review.due + 복습 통계 대시보드 + 세션 완료 API + Stats 히트맵/리텐션 | 리마인더 발행, 통계 API, `PUT /reviews/sessions/{sessionId}/complete`, heatmap/retention API | notification (platform W3) |
| W4 | 06-01~05 | 버그 수정 + 통합 테스트 | 안정화 | 전체 통합 (W3) |
| W5 | 06-08~12 | 복습/SRS/Kafka 이벤트 E2E + P0 버그 수정 | 복습 E2E 결과, Kafka 안정화 리포트, P0 수정 PR | engagement XP, platform notification |

## 협업 인터페이스

| 상대 | 주고받는 것 | 방향 |
|------|------------|------|
| @engagement-owner | card.reviewed 이벤트 → XP 적립 | 발행 → |
| @platform-owner | card.review.due 이벤트 → 알림 발송 | 발행 → |
| @learning-ai-owner | AI가 생성한 카드를 card 모듈에 저장 | ← 수신 |
| Frontend | 복습 세션 UI 데이터 제공 | 제공 → |

## 성공 기준

- [ ] 덱/카드 CRUD 완전 동작
- [ ] SM-2 알고리즘 정확 계산 (4개 rating × 경계값 테스트)
- [ ] 복습 세션 플로우 (카드 제시 → 답변 → rating → 다음 복습일)
- [ ] Kafka 이벤트 발행 (card.reviewed, card.review.due)
- [ ] 복습 통계 API 동작
- [ ] 카드 일괄 생성 (`POST /cards/batch`) 동작 *(Wiki API 명세서 동기화 — 추가)*
- [ ] 복습 큐 조회 (`GET /reviews/queue`) 동작 *(Wiki API 명세서 동기화 — 추가)*
- [ ] 세션 완료 API (`PUT /reviews/sessions/{sessionId}/complete`) 동작 *(Wiki API 명세서 동기화 — 추가)*
- [ ] Stats 히트맵 (`GET /stats/heatmap`) 및 리텐션 커브 (`GET /stats/retention`) API 동작 *(Wiki API 명세서 동기화 — 추가)*

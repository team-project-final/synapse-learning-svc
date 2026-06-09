# 발표 데모 시나리오 — learning-card

> **발표일**: 2026-06-15 (월)
> **서비스 포트**: 8084
> **시드 데이터 실행 조건**: `--spring.profiles.active=demo`

---

## 사전 준비

### 1. Docker Compose 기동

```bash
cd learning-card
docker-compose up -d
```

서비스가 모두 healthy 상태인지 확인:

```bash
docker-compose ps
```

### 2. 시드 데이터 확인용 상수

| 항목 | 값 |
|------|----|
| 포트 | `8084` |
| DEMO_TENANT_ID | `a0000000-0000-0000-0000-000000000001` |
| DEMO_USER_ID | `d0000000-0000-0000-0000-000000000001` |
| DEMO_DECK_ID | `e0000000-0000-0000-0000-000000000001` |
| 카드 수 | 5장 (review 2, learning 1, new 2) |
| 복습 세션 | 2개 완료 |

---

## 시나리오 1 — 덱 및 카드 조회

### 덱 목록 조회

```bash
curl -s -X GET http://localhost:8084/decks \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

**기대 응답**: `Java & Spring 핵심 개념` 덱 1개

### 덱 내 카드 목록 조회

```bash
curl -s -X GET "http://localhost:8084/decks/e0000000-0000-0000-0000-000000000001/cards" \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

**기대 응답**: 카드 5장 (status 분포: review 2장, learning 1장, new 2장)

---

## 시나리오 2 — 복습 큐 조회

오늘 복습 대상 카드를 확인한다.

```bash
curl -s -X GET "http://localhost:8084/reviews/queue?deckId=e0000000-0000-0000-0000-000000000001" \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

**기대 응답**: `due_date ≤ 오늘`인 카드 — 카드 3(@Transactional), 카드 4(Kafka), 카드 5(SM-2) 포함

---

## 시나리오 3 — 복습 세션 시작 → SM-2 계산

### 세션 시작

```bash
curl -s -X POST http://localhost:8084/reviews/sessions \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"deckId": "e0000000-0000-0000-0000-000000000001"}' | jq .
```

응답에서 `sessionId`를 복사한다. (이하 `<SESSION_ID>`)

### 카드 rating 제출 — GOOD(3)

```bash
curl -s -X POST "http://localhost:8084/reviews/sessions/<SESSION_ID>/submit" \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "cardId": "c0000000-0000-0000-0000-000000000004",
    "rating": 3,
    "timeSpentMs": 7000
  }' | jq .
```

**설명 포인트**: `nextReviewDate`가 오늘 + `interval_days`로 계산된 날짜가 됨 (SM-2 알고리즘)

| rating 값 | 의미 | interval 변화 |
|-----------|------|----------------|
| 1 (Again) | 완전히 모름 | 1일로 리셋 |
| 2 (Hard)  | 어렵게 기억 | 유지 |
| 3 (Good)  | 정확히 기억 | × EF |
| 4 (Easy)  | 매우 쉬움 | × EF × 2 |

### 세션 완료

```bash
curl -s -X PUT "http://localhost:8084/reviews/sessions/<SESSION_ID>/complete" \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

---

## 시나리오 4 — 통계 대시보드 조회

복습 후 통계가 반영되었는지 확인한다.

```bash
curl -s -X GET http://localhost:8084/stats/overview \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

**기대 응답 (시드 데이터 기준)**:

```json
{
  "data": {
    "totalReviews": 5,
    "todayReviews": 0,
    "weeklyReviews": 5,
    "overallCorrectRate": 100.0,
    "currentStreak": 0,
    "longestStreak": 0
  }
}
```

히트맵 조회:

```bash
curl -s -X GET http://localhost:8084/stats/heatmap \
  -H "X-User-Id: d0000000-0000-0000-0000-000000000001" \
  -H "X-Tenant-Id: a0000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer <JWT>" | jq .
```

---

## 시나리오 5 — Kafka 이벤트 발행 확인 (선택)

복습 완료 시 `learning.card.review-completed-v1` 토픽에 이벤트가 발행된다.

```bash
# Kafka 토픽 메시지 확인 (docker-compose 환경)
docker exec -it learning-card-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic learning.card.review-completed-v1 \
  --from-beginning \
  --max-messages 5
```

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|------|------|------|
| 401 Unauthorized | JWT 토큰 만료/누락 | 새 토큰 발급 후 Bearer 헤더 추가 |
| 복습 큐가 비어있음 | 시드 데이터 미적용 | `demo` 프로파일 여부 확인 |
| Kafka 연결 실패 | `synapse.kafka.enabled=false` | docker-compose에서 KAFKA_ENABLED=true 확인 |
| 통계가 0으로 표시 | 캐시 미갱신 | 5분 TTL 만료 후 재조회 또는 Redis flush |

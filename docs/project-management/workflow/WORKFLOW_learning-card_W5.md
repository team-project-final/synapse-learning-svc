# WORKFLOW: @learning-card-owner — Week 5

> **Task 문서**: [TASK_learning-card.md](../task/TASK_learning-card.md)
> **기간**: 2026-06-08 ~ 2026-06-12, 5 영업일
> **목표**: 복습/SRS/Kafka 이벤트 E2E 및 P0 버그 수정

## Step 9: 복습 E2E

### 9.1 시나리오 정의
- [x] 덱 생성 → 카드 생성 → 복습 큐 조회 시나리오 작성 (`ReviewFlowE2ETest`)
- [x] 복습 제출 → SM-2 계산 → 다음 복습일 갱신 시나리오 작성 (`ReviewFlowE2ETest`)
- [x] 세션 완료 → 통계 반영 시나리오 작성 (`ReviewStatsPostgresE2ETest`)
- [x] note.created → AI 카드 자동 생성 → 덱 저장 시나리오 작성 (`AiCardGenerationE2ETest`)

### 9.2 실행 및 수정
- [x] 복습 전체 플로우 E2E 테스트 실행 — BUILD SUCCESSFUL
- [x] 알고리즘 경계값 테스트 실행 (`Sm2CalculatorTest`)
- [x] P0 버그 수정 — H2 공유 컨텍스트 스키마 DROP 버그 수정 (KafkaEventFlowE2ETest 독립 DB 분리)

## Step 10: Kafka 이벤트 안정화

### 10.1 이벤트 검증
- [x] `card.reviewed` 발행 확인 — `KafkaEventFlowE2ETest` (Avro 역직렬화 필드 검증)
- [x] `card.review.due` 발행 확인 — `KafkaEventFlowE2ETest` (Avro 역직렬화 필드 검증)
- [x] 파티션 키 = userId 보장 확인 — `KafkaEventFlowE2ETest`
- [ ] engagement/platform 소비 연동 확인 (라이브 Docker Compose E2E — Day 2)

### 10.2 발표 준비
- [ ] 발표용 덱/카드/복습 데이터 준비
- [ ] 복습 데모 시나리오 검증

## Done When

- [x] 복습 E2E가 통과한다. (91 tests, 0 failed)
- [x] card 이벤트 발행 및 Avro 필드 검증 완료
- [x] AI 카드 자동 생성 → 덱 저장 E2E 통과
- [x] learning-card P0 버그가 0건이다.
- [ ] Docker Compose 라이브 E2E engagement/platform 소비 연동 확인
- [ ] 발표용 복습 데모가 안정적으로 동작한다.

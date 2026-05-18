# WORKFLOW: @learning-card-owner — Week 5

> **Task 문서**: [TASK_learning-card.md](../task/TASK_learning-card.md)
> **기간**: 2026-06-08 ~ 2026-06-12, 5 영업일
> **목표**: 복습/SRS/Kafka 이벤트 E2E 및 P0 버그 수정

## Step 9: 복습 E2E

### 9.1 시나리오 정의
- [ ] 덱 생성 → 카드 생성 → 복습 큐 조회 시나리오 작성
- [ ] 복습 제출 → SM-2 계산 → 다음 복습일 갱신 시나리오 작성
- [ ] 세션 완료 → 통계 반영 시나리오 작성

### 9.2 실행 및 수정
- [ ] 복습 전체 플로우 E2E 테스트 실행
- [ ] 알고리즘 경계값 테스트 실행
- [ ] P0 버그 수정 및 회귀 테스트

## Step 10: Kafka 이벤트 안정화

### 10.1 이벤트 검증
- [ ] `card.reviewed` 발행 확인
- [ ] `card.review.due` 발행 확인
- [ ] engagement/platform 소비 연동 확인

### 10.2 발표 준비
- [ ] 발표용 덱/카드/복습 데이터 준비
- [ ] 복습 데모 시나리오 검증

## Done When

- [ ] 복습 E2E가 통과한다.
- [ ] card 이벤트 발행과 소비 연동이 확인된다.
- [ ] learning-card P0 버그가 0건이다.
- [ ] 발표용 복습 데모가 안정적으로 동작한다.

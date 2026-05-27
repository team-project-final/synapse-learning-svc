# WORKFLOW: @learning-ai-owner — Week 3

> **Task 문서**: [TASK_learning-ai.md](../task/TASK_learning-ai.md)
> **기간**: 2026-05-26 ~ 2026-05-29, 4 영업일
> **PRD**: [PRD_W3.md](../prd/PRD_W3.md)

---

## Step 6: AI 카드 자동 생성 — note.created Kafka 소비 → LLM → Card 생성 → learning-card API 호출

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W3 해당 요구사항 확인 (AI 카드 자동 생성)
- [x] Duration 산정 확인

### 1.2 요구사항 분석
- [x] note.created Kafka 이벤트 스키마 분석 (event_id, note_id, user_id, tenant_id, deck_id)
- [x] LLM 프롬프트 설계 (노트 내용 → Q&A 카드 생성, 3~5장)
- [x] learning-card API 호출 스펙 분석 (POST /decks/{deck_id}/cards)
- [x] 생성 실패 시 재시도/DLQ 전략 정의 (3회, 2s→4s→8s, DLQ: note.created.dlq)

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: No (내부 Kafka 소비 + 서비스 간 호출)
- [x] LLM API 키 관리 (pydantic-settings, .env)
- [x] 서비스 간 인증 (X-User-Id / X-Tenant-Id 헤더)

### 1.4 아키텍처 설계
- [x] 파이프라인 설계: note.created 소비 → NoteApiClient(노트 조회) → LLM → CardApiClient(카드 저장)
- [x] LLM 모델: Claude (claude-3-5-sonnet-20240620), Jinja2 프롬프트 템플릿
- [x] 비동기 처리: asyncio.create_task로 consume loop 분리

### 1.5 Security 2차 검토
- [x] LLM 응답 검증 (JSON 파싱 + Pydantic 모델 검증)
- [x] 비용 제어: 기존 `@track_tokens` 데코레이터 유지

### 1.6 DTO / Entity 설계
- [x] `NoteCreatedEvent` DTO (`app/kafka/schemas.py`)
- [x] `GeneratedCard` / `CardGenerateResponse` (기존 `app/schemas/ai.py` 활용)

### 1.7 Client 구현
- [x] `NoteApiClient` 구현 (`app/clients/note_client.py`)
- [x] `CardApiClient` 구현 (`app/clients/card_client.py`) — W2에서 구현 완료

### 1.8 Service + Test
- [x] `AiCardKafkaConsumer` 구현 (`app/kafka/consumer.py`)
  - consumer group: `learning-ai-card-generator`
  - 재시도 3회 (tenacity AsyncRetrying), DLQ, idempotency(인메모리 set), 60초 타임아웃
- [x] `AiCardPipelineService` 구현 (`app/services/card_pipeline_service.py`) — W2에서 구현 완료
- [x] lifespan으로 Consumer 시작/종료 연결 (`app/main.py`)
- [x] 단위 테스트 4개 작성 및 통과 (`tests/test_kafka_consumer.py`)

### 1.9 E2E 검증
- [ ] Docker Compose 환경에서 note.created 이벤트 발행 → 카드 자동 생성 확인 (W4에서 진행 예정)

### 1.10 결과 정리
- [x] REPORT.md 업데이트
- [x] HISTORY / WORKFLOW 문서 갱신

**Step 6 Status**: [x] Done — 2026-05-27

---

## Step 7: RAG Q&A (시간 허용 시) — 관련 청크 검색 → LLM 답변 생성 + 시맨틱 캐시

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W3 해당 요구사항 확인 (RAG Q&A)
- [x] Duration 산정 확인

### 1.2 요구사항 분석
- [x] RAG 파이프라인 분석 (질문 임베딩 → Redis 캐시 → pgvector 검색 → LLM → 답변)
- [x] 시맨틱 캐시 요건 분석 (코사인 유사도 ≥ 0.95 → LLM 호출 스킵)
- [x] 청크 검색 전략 (pgvector top-K=5, threshold=0.7)

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: X-User-Id 헤더 기반 (기존 패턴 준수)
- [x] 시맨틱 캐시 tenant_id 기반 격리 적용

### 1.4 아키텍처 설계
- [x] RAG 파이프라인: 임베딩 → Redis 캐시 → pgvector → Claude LLM → 캐싱
- [x] 시맨틱 캐시: Redis `rag_cache:{tenant_id}`, numpy 코사인 유사도, TTL 3600s, 최대 100항목
- [x] 프롬프트 템플릿: `app/prompts/qa/system.txt` + `user.jinja2` (context + question)

### 1.5 Security 2차 검토
- [x] 시맨틱 캐시 tenant_id 단위 격리 (사용자 간 데이터 누출 방지)

### 1.6 DTO / Entity 설계
- [x] `QaRequest` (question, stream)
- [x] `QaSource` (chunk_id, note_id, content, score)
- [x] `QaResponse` (answer, sources, from_cache)

### 1.7 Repository / Client 구현
- [x] `NoteChunkRepository.search_similar` 재사용 (Step 4 구현체)
- [x] Redis 클라이언트: `redis.asyncio.Redis`, decode_responses=True

### 1.8 Service + Test
- [x] `RagService` 구현 (`app/services/rag_service.py`)
- [x] `ClaudeService.generate_qa` / `stream_qa` 추가
- [x] 컨텍스트 최대 12000자(≈3000 토큰) 제한
- [x] 전체 테스트 12개 통과 (기존 테스트 회귀 없음)

### 1.9 Controller + Test
- [x] `POST /ai/qa` 엔드포인트 (`app/api/ai.py`)
- [x] `stream=true` 시 `StreamingResponse(media_type="text/event-stream")` 반환
- [x] SSE 포맷: `data: {"text": "..."}\n\n`, 마지막 `data: [DONE]\n\n`

### 1.10 결과 정리
- [x] REPORT.md 업데이트
- [x] HISTORY / WORKFLOW 문서 갱신

**Step 7 Status**: [x] Done — 2026-05-27

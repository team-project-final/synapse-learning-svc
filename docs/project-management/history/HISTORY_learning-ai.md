# Work History: @learning-ai

> **담당**: learning-ai / AI 연동 (Claude, Embedding)  
> **관련 문서**: [SCOPE](../scope/SCOPE_learning-ai.md) | [TASK](../task/TASK_learning-ai.md) | [WORKFLOW](../workflow/WORKFLOW_learning-ai_W1.md)

---

## 진행 상태 대시보드

### W1 (2026-05-12 ~ 05-16)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 1 | FastAPI scaffolding | DONE | 2026-05-12 | 2026-05-15 | |
| Step 2 | Anthropic Claude API 연동 | DONE | 2026-05-13 | 2026-05-15 | |
| Step 3 | OpenAI Embedding API 연결 | DONE | 2026-05-14 | 2026-05-15 | |

**W1 진행률**: 3/3 Steps 완료 (100%)

### W2 (2026-05-19 ~ 05-23)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 4 | 시맨틱 검색 구현 | DONE | 2026-05-18 | 2026-05-18 | |
| Step 5 | AI 카드 생성 골격 | DONE | 2026-05-18 | 2026-05-18 | |

**W2 진행률**: 2/2 Steps 완료 (100%)

### W3 (2026-05-26 ~ 05-30)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 6 | note.created Kafka 이벤트 소비 | DONE | 2026-05-27 | 2026-05-27 | AiCardKafkaConsumer, NoteApiClient |
| Step 7 | RAG 기반 질문 답변 (P2) | DONE | 2026-05-27 | 2026-05-27 | RagService, Redis 시맨틱 캐시 |

**W3 진행률**: 2/2 Steps 완료 (100%)

### W4 (2026-06-02 ~ 06-06)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 8 | AI 카드 자동 생성 E2E 테스트 | Not Started | — | — | |
| Step 9 | 시맨틱 검색 정확도 검증 | Not Started | — | — | |

**W4 진행률**: 0/2 Steps 완료

---

## 작업 로그

### W1 (2026-05-12 ~ 05-16)

#### 2026-05-15 (목)
- **완료**:
  - `app/core/config.py` 기반 설정 관리 표준화
  - LLM Timeout, Retry(Tenacity), Fallback(AIService) 구현
  - API 응답 표준 래퍼(`ApiResponse`) 및 전역 에러 핸들러 적용
  - 토큰 추적 및 비용 제어 로직 추가
- **이슈**: Anthropic API 지연 시 Fallback 정상 동작 확인

### W2 (2026-05-19 ~ 05-23)

#### 2026-05-18 (월)
- **완료**:
  - `pgvector` 기반 시맨틱 검색(`POST /api/v1/ai/search/semantic`) 구현
  - LLM 기반 플래시카드 자동 생성(`POST /api/v1/ai/cards/generate`) 구현
  - 프롬프트 외부 파일 분리 및 Jinja2 템플릿 적용

#### 2026-05-19 (화)
- **완료**:
  - **W2 아키텍처 진전**: 에러 코드 접두사 `L_` 적용 (`INTERNAL_ERROR` -> `L_INTERNAL_ERROR` 등)
  - `RequestValidationError` 핸들러 추가 (`L_VALIDATION_FAILED`)로 유효성 검사 실패 시에도 표준 응답 포맷 준수
  - `synapse-svc-template` 문서 분석 및 `learning-ai` 적용 로드맵 수립
- **다음**: W3 Kafka 이벤트 수신 및 비동기 카드 생성 구현

### W3 (2026-05-26 ~ 05-30)

#### 2026-05-26 (월)
- **완료**: 없음 (부처님오신날 — 공휴일)

#### 2026-05-27 (화)
- **완료**:
  - **Step 6**: `note.created.v1` Kafka Consumer 구현 완료
    - `app/kafka/schemas.py`: `NoteCreatedEvent` DTO (event_id, note_id, user_id, tenant_id, deck_id)
    - `app/kafka/consumer.py`: `AiCardKafkaConsumer` — 재시도 3회(2s→4s→8s), DLQ(`note.created.dlq`), idempotency(인메모리 set), 60초 타임아웃
    - `app/clients/note_client.py`: `NoteApiClient` — knowledge-svc 노트 내용 조회
    - `app/main.py`: lifespan으로 Kafka Consumer 시작/종료 연결 (`kafka_enabled` 플래그 지원)
    - `app/core/config.py`: Kafka 설정 5개 + `note_service_url` 추가
    - `tests/test_kafka_consumer.py`: 단위 테스트 4개 (happy path, 중복 skip, 스키마 오류 DLQ, 파이프라인 실패 DLQ)
  - **Step 7**: RAG Q&A 구현 완료
    - `app/services/rag_service.py`: `RagService` — 임베딩 → Redis 캐시 확인 → pgvector top-K=5 검색 → Claude LLM → 캐싱
    - `app/services/claude_service.py`: `generate_qa` / `stream_qa` 메서드 추가
    - `app/schemas/ai.py`: `QaRequest`, `QaSource`, `QaResponse` 추가
    - `app/prompts/qa/`: Q&A 시스템·사용자 프롬프트 파일 추가
    - `app/api/ai.py`: `POST /ai/qa` 엔드포인트 추가 (stream=true 시 SSE)
    - `app/api/deps.py`: `get_redis_client`, `get_rag_service` 의존성 추가
    - `app/core/config.py`: `redis_url` 추가
    - Redis 시맨틱 캐시: `rag_cache:{tenant_id}`, 코사인 유사도 ≥ 0.95, TTL 3600s, 최대 100항목
- **브랜치**: `feature/LEARN-AI-W3-kafka-consumer` (2커밋 push 완료)
- **다음**: PR 작성 → dev 머지

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
| 2026-05-27 | W3 Step 6·7 완료 반영 (대시보드 + 작업 로그) |
| 2026-05-19 | HISTORY 문서 Step 분류 오류 수정 (Step 6을 W2에서 W3로 이동) |
| 2026-05-11 | W2/W3/W4 대시보드 및 로그 템플릿 추가 |
| 2026-05-11 | 초기 템플릿 생성 |

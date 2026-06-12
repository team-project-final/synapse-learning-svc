# 작업 보고서: 이슈 #85 — learning-ai Prometheus 메트릭 엔드포인트 추가 (2026-06-12)

## 2026-06-12 — `/metrics` 엔드포인트 노출

### 배경
EKS ServiceMonitor 스크랩 시 learning-ai `/actuator/prometheus` 404 → `TargetDown` false-positive.
Python 서비스는 Spring actuator 없음. `prometheus-fastapi-instrumentator`로 `/metrics` 노출.

### 변경 파일
- `pyproject.toml`: `prometheus-fastapi-instrumentator>=7.0.0` 의존성 추가 + mypy override 추가
- `app/main.py`: `Instrumentator().instrument(app).expose(app, endpoint="/metrics")` 한 줄 추가

### 이전 vs 이후
- 이전: `GET /actuator/prometheus` → 404 (Python 서비스에 Spring actuator 없음)
- 이후: `GET /metrics` → 200 + Prometheus 텍스트 포맷 (요청수·레이턴시·상태코드 자동 계측)

---

# 작업 보고서: 이슈 #73·#77·#78 수정 (2026-06-12)

## 2026-06-12 — AI 키 Graceful Gate + 모델 ID 교체 + Kafka 파이프라인 계약 수정

### 배경
W5 E2E 라이브에서 발견된 3개 이슈 해결.

### 변경 파일
- `app/core/config.py`: `anthropic_model` 설정 필드 + `ai_enabled`/`openai_enabled` computed property 추가
- `app/api/deps.py`: 키 미설정 시 HTTP 503 반환 (기존: 빈 문자열로 클라이언트 생성 → 호출 시점 인증 실패)
- `app/services/claude_service.py`: 하드코딩 `"claude-3-5-sonnet-20240620"` → `settings.anthropic_model` (3곳) + `settings` import 추가
- `app/kafka/consumer.py`: `EventHandlerFn` Protocol에 `content: str | None` 추가 + pipeline_fn 호출에 `content=event.content` 전달
- `app/main.py`: `or ""` 제거, 키 없으면 WARN 로그 + consumer 미기동. `NoteApiClient` 호출 제거 — Kafka 이벤트 `content` 필드 직접 사용
- `.env.example`: `LEARNING_AI_ANTHROPIC_MODEL=claude-sonnet-4-6` 추가

### 이슈별 근거

**#73 (AI 키 Graceful Gate)**
- 이전: `settings.anthropic_api_key or ""`로 빈 키 클라이언트 생성 → 기동 정상이나 AI 호출 시 모호한 인증 에러
- 이후: 키 없으면 REST 엔드포인트 503, Kafka consumer 미기동, 기동 시 WARN 로그

**#77 (모델 ID 폐기)**
- 이전: `claude-3-5-sonnet-20240620` 하드코딩 → Anthropic 404 `not_found_error`
- 이후: `settings.anthropic_model` 변수 참조 (기본값 `claude-sonnet-4-6`), `LEARNING_AI_ANTHROPIC_MODEL` env로 외부화

**#78 (note 본문 fetch 계약 불일치)**
- 원인 재분석: `NoteCreatedEvent.content`(Kafka 이벤트 본문)가 존재하나 `pipeline_fn`에 전달되지 않아 불필요한 HTTP 호출 발생
- 이전: `NoteApiClient.get_note_content()` HTTP 호출 → knowledge-svc `/notes/{uuid}` 경로·ID타입·JWT 3중 불일치로 100% 500 → DLQ
- 이후: Kafka 이벤트 `content` 필드 직접 사용. knowledge-svc 코드 변경 없음

---

# 작업 보고서: 컨테이너 비-root 실행 (2026-06-03)

## 2026-06-03 — 컨테이너 비-root 실행
- Dockerfile runtime stage에 uid/gid 101 `app` 유저 생성(groupadd/useradd) + `/app` chown + `USER app` 추가.
- 근거: gitops base의 `runAsNonRoot:true`(B2 보안 하드닝) 대응. uvicorn은 비-root로 /app(읽기)+/usr/local(읽기)만 사용.
- 이전: root 실행(USER 미지정).

---

# 작업 보고서: learning-ai 규정 준수 및 리팩토링 (2026-05-15)

## 1. 개요
`synapse-learning-svc/learning-ai` 프로젝트의 코드를 최신 프로젝트 규정(`13-python-llm.md`, `공통_개발_규칙.md`)에 맞게 전면 리팩토링하였습니다.

## 2. 주요 변경 사항

### 설정 및 구조 개선 (Phase 1)
- `app/core/settings.py` -> `app/core/config.py` 파일명 변경 및 관련 임포트 전수 수정.
- `app/prompts/` 디렉토리를 신설하여 하드코딩된 프롬프트를 `system.txt`, `user.jinja2`로 분리 관리.

### LLM 안정성 및 비용 제어 (Phase 2)
- **Timeout 적용**: 모든 LLM 클라이언트에 `httpx.Timeout(30.0, connect=5.0)` 적용.
- **Retry 로직**: `tenacity` 라이브러리를 도입하여 `RateLimitError`, `APIConnectionError`, `InternalServerError` 발생 시 지수 백오프(Exponential Backoff) 기반 3회 재시도 구현.
- **Fallback 구현**: Anthropic 서비스 실패 시 OpenAI(`gpt-4o-mini`)로 자동 전환되는 Fallback 오케스트레이터(`AIService`) 구현.
- **비용 제어**: `@track_tokens` 데코레이터를 구현하여 토큰 사용량을 로깅하고 일일 사용 한도(500,000 tokens) 초과 시 차단하는 방어 로직 추가.

### API 응답 표준화 (Phase 3)
- **응답 래퍼**: 모든 API 응답을 `{"success": true, "data": ..., "meta": ...}` 표준 포맷으로 감싸도록 수정.
- **에러 핸들링**: `app/core/exceptions.py`를 전면 수정하여 모든 예외 상황에서 표준 에러 응답(`{"success": false, "error": {...}}`)을 반환하도록 통일.

### 테스트 코드 및 품질 관리 (Phase 4)
- **Pytest 업데이트**: 변경된 API 응답 포맷 및 Retry/Fallback 로직에 맞춰 `tests/test_ai.py`의 검증 로직을 전면 수정.
- **품질 도구**: `tenacity`, `jinja2` 등 누락된 의존성을 `pyproject.toml`에 추가하고 `ruff`, `mypy`를 통한 정적 분석 수행 및 주요 이슈 해결.

### 기술 부채 해결 및 설정 정리 (2026-05-15)
- **설정 템플릿 정리**: 중복 및 파일명 오류가 있었던 `.env.example` 파일들을 정리하였습니다.
  - 잘못된 형식의 `learning-ai/.env.example` 삭제.
  - 파일명에 공백이 포함되어 있던 `learning-ai/ .env.example`을 올바른 이름(`.env.example`)으로 변경.
  - 최신 `config.py`의 `LEARNING_AI_` 접두사 규칙이 반영된 템플릿을 최종 유지.

## 3. 초기 상태와의 비교

| 항목 | 초기 상태 | 변경 후 상태 |
|---|---|---|
| 설정 관리 | `settings.py` (비표준) | `config.py` (표준 준수) |
| 프롬프트 | 코드 내 하드코딩 | `prompts/` 폴더 내 파일로 관리 (Jinja2) |
| 재시도 로직 | 수동 `for` 루프 | `tenacity` 데코레이터 (선언적) |
| API 응답 | 원본 데이터 직접 반환 | `ApiResponse` 표준 래퍼 사용 |
| 에러 처리 | 단순 텍스트/객체 반환 | `ApiErrorResponse` 표준 규격 준수 |
| 비용 제어 | 추적 기능 없음 | 실시간 토큰 추적 및 일일 한도 제어 |

## 4. 향후 과제
- Redis 도입 시 `app/core/logging.py`의 `_daily_tokens`를 Redis 기반으로 변경하여 다중 인스턴스 환경 지원 필요.
- `alembic` 및 `models` 레이어의 Mypy 엄격 타입 체크 보강.

---

# 작업 보고서: 시맨틱 검색 (Step 4) 구현 (2026-05-18)

## 1. 개요
`pgvector`를 활용하여 노트 조각(Note Chunks)에 대한 코사인 유사도 기반 시맨틱 검색 기능을 구현하였습니다.

## 2. 주요 변경 사항

### 데이터 모델 및 저장소 (Step 4.1)
- **Repository 확장**: `NoteChunkRepository`에 `search_similar` 메서드를 추가하였습니다.
- **pgvector 연동**: SQLAlchemy의 `pgvector` 확장을 사용하여 코사인 유사도 연산자(`<=>`) 기반의 쿼리를 구현하였습니다. 유사도 점수는 `1 - distance`로 계산하여 반환합니다.

### 서비스 레이어 (Step 4.2)
- **오케스트레이션**: `AIService`에 `semantic_search` 로직을 추가하였습니다.
- **임베딩 변환**: 사용자의 검색 질의어를 `OpenAIEmbeddingService`를 통해 1536차원 벡터로 변환한 후 저장소에 검색을 요청합니다.

### API 엔드포인트 (Step 4.3)
- **엔드포인트 신설**: `POST /api/v1/ai/search/semantic`을 추가하여 시맨틱 검색 기능을 노출하였습니다.
- **DTO 정의**: `SemanticSearchRequest`, `SemanticSearchResponse`, `SemanticSearchResult` 등 Pydantic 모델을 정의하여 입출력 규격을 표준화하였습니다.

### 품질 관리 및 테스트 (Step 4.4)
- **단위 테스트**: `test_note_chunk_repository.py`에 벡터 검색 정확도 검증 테스트를 추가하였습니다.
- **통합 테스트**: `test_ai.py`에 API 엔드포인트 호출 및 모킹된 결과 반환 테스트를 추가하였습니다.
- **정적 분석**: `ruff` 및 `mypy`를 실행하여 코드 스타일과 타입 안정성을 확인하였습니다.

## 3. 기술적 근거
- **HNSW 인덱스**: `NoteChunk` 모델에 이미 설정된 HNSW 인덱스를 활용하여 대규모 데이터에서도 빠른 검색 성능을 보장합니다.
- **Tenant 격리**: 검색 쿼리에 `tenant_id` 필터를 강제하여 다중 사용자 환경에서의 데이터 보안을 유지합니다.

## 4. 향후 과제
- Step 5: 노트 내용을 바탕으로 LLM을 이용한 플래시카드 자동 생성 기능 구현 예정.
- 검색 품질 튜닝: 검색 결과의 정밀도를 높이기 위해 임베딩 모델의 파라미터나 검색 임계값(Threshold)을 조정하는 실험 필요.

---

# 작업 보고서: AI 플래시카드 자동 생성 (Step 5) 구현 (2026-05-18)

## 1. 개요
LLM(Claude/OpenAI)을 활용하여 사용자의 노트 내용으로부터 학습용 플래시카드(질문-답변 쌍)를 자동으로 생성하는 기능을 구현하였습니다.

## 2. 주요 변경 사항

### 프롬프트 엔지니어링 (Step 5.1)
- **프롬프트 분리**: `app/prompts/card_generation/` 디렉토리를 신설하여 시스템 및 사용자 프롬프트를 외부 파일로 관리합니다.
- **구조화된 출력**: AI가 반드시 JSON 리스트 형식(`[{"front": "...", "back": "..."}]`)으로 응답하도록 페르소나와 제약 조건을 설정하였습니다.

### 서비스 레이어 확장 (Step 5.2)
- **AIService.generate_cards**: 노트 본문을 입력받아 LLM 호출, 응답 정제(Markdown 제거 등), JSON 파싱 및 Pydantic 모델 변환 과정을 수행합니다.
- **파싱 안정성**: AI 응답에 포함될 수 있는 마크다운 코드 블록 등을 제거하고 순수 JSON만 추출하는 클리닝 로직을 적용하였습니다.

### API 엔드포인트 및 DTO (Step 5.3)
- **엔드포인트 고도화**: `POST /api/v1/ai/cards/generate` 엔드포인트를 실제 생성 로직과 연결하였습니다.
- **DTO 정의**: `GeneratedCard`, `CardGenerateResponse` 스키마를 추가하여 생성된 카드의 품질(글자 수 제한 등)을 검증합니다.

### 품질 관리 및 테스트 (Step 5.4)
- **통합 테스트**: `test_ai.py`에 실제 JSON 응답 모킹을 통한 플래시카드 생성 전체 흐름 테스트를 추가하였습니다.
- **정적 분석**: `ruff` 및 `mypy`를 통한 스타일 및 타입 체크를 완료하였습니다.

## 3. 기술적 근거
- **JSON Mode**: 시스템 프롬프트를 통해 출력을 JSON으로 강제하여 프로그래밍 방식의 후처리가 용이하도록 설계하였습니다.
- **Fallback 유지**: 1주차에 구현된 Claude -> OpenAI 자동 전환 로직을 그대로 활용하여 서비스 가용성을 높였습니다.

## 4. 향후 과제

---

# 작업 보고서: W2 아키텍처 진전 (ErrorCode 접두사 적용) (2026-05-19)

## 1. 개요
전체 프로젝트의 W2(Global Infrastructure) 표준 규정에 맞게 `learning-ai` 서비스의 에러 코드를 고도화하였습니다.

## 2. 주요 변경 사항

### 에러 코드 표준화 (Step W2.1)
- **접두사 적용**: `learning` 서비스 그룹의 고유 식별자인 `L_` 접두사를 모든 API 에러 코드에 적용하였습니다.
  - `INTERNAL_ERROR` -> `L_INTERNAL_ERROR`
  - `HTTP_ERROR` -> `L_HTTP_ERROR`

## 3. 기술적 근거
- **서비스 식별성**: 클라이언트 응답만으로 어느 마이크로서비스에서 에러가 발생했는지 즉각적으로 식별할 수 있습니다.
- **코드 충돌 방지**: `knowledge` 서비스(`KN_`)나 `platform` 서비스(`P_`) 등 타 서비스와의 에러 코드 중복 가능성을 원천 차단합니다.

## 4. 향후 과제
- W3: Kafka 이벤트 통신 도입을 통한 도메인 간 결합도 해소 예정.

---

# 작업 보고서: Kafka Consumer (Step 6) 구현 (2026-05-27)

## 1. 개요
`note.created.v1` Kafka 이벤트를 소비하여 AI 카드를 자동 생성하고 learning-card API에 저장하는 비동기 파이프라인을 구현하였습니다.

## 2. 주요 변경 사항

### 신규 파일

| 파일 | 내용 |
|---|---|
| `app/kafka/__init__.py` | Kafka 모듈 패키지 |
| `app/kafka/schemas.py` | `NoteCreatedEvent` DTO (event_id, note_id, user_id, tenant_id, deck_id) |
| `app/kafka/consumer.py` | `AiCardKafkaConsumer` — 이벤트 소비 + 재시도 + DLQ + idempotency |
| `app/clients/note_client.py` | `NoteApiClient` — knowledge-svc에서 노트 내용 조회 |
| `tests/test_kafka_consumer.py` | Consumer 단위 테스트 4개 |

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `pyproject.toml` | `aiokafka>=0.11.0` 추가, mypy override 추가 |
| `app/core/config.py` | Kafka 설정 5개(`kafka_enabled`, `kafka_bootstrap_servers` 등) + `note_service_url` 추가 |
| `app/main.py` | `@asynccontextmanager` lifespan 추가 → 앱 시작/종료 시 Kafka Consumer 연결 |

## 3. 핵심 설계 결정

### Consumer 동작 흐름
```
메시지 수신
  → NoteCreatedEvent 스키마 검증 (실패 시 즉시 DLQ)
  → event_id 중복 확인 (이미 처리 시 skip)
  → _process_with_retry (최대 3회, 2s→4s→8s 지수 백오프, 60초 타임아웃)
      → NoteApiClient.get_note_content
      → AiCardPipelineService.generate_and_save (LLM → 카드 저장)
  → 성공 시 event_id를 _processed set에 추가
  → 실패 시 DLQ 전송
  → 오프셋 커밋 (always)
```

### 주요 제약 준수
- Consumer group: `learning-ai-card-generator`
- 재시도: 3회 (2s, 4s, 8s) — tenacity `AsyncRetrying`
- DLQ 토픽: `note.created.dlq`
- 이벤트 처리 타임아웃: 60초 (`asyncio.wait_for`)
- 오프셋 커밋: 수동(`enable_auto_commit=False`), 처리 완료 또는 DLQ 전송 후

### 설계 선택 근거
- **idempotency 인메모리**: Redis는 Step 7 범위이므로 W3에서는 프로세스 로컬 `set` 사용
- **note 조회 위치**: Consumer 클로저에서 처리 — `AiCardPipelineService` 기존 인터페이스(`note_content` 직접 수신) 유지하여 기존 테스트 무결성 보장
- **kafka_enabled flag**: Kafka 없는 로컬/테스트 환경에서 `LEARNING_AI_KAFKA_ENABLED=false`로 앱 기동 가능

## 4. 테스트 결과
```
tests/test_kafka_consumer.py::test_handle_message_happy_path        PASSED
tests/test_kafka_consumer.py::test_handle_message_duplicate_skipped PASSED
tests/test_kafka_consumer.py::test_handle_message_invalid_schema_sends_to_dlq PASSED
tests/test_kafka_consumer.py::test_handle_message_pipeline_failure_sends_to_dlq PASSED

전체 12 passed (기존 테스트 모두 통과)
```

## 5. 향후 과제
- idempotency를 Redis 기반으로 전환 시 다중 인스턴스 환경 지원 가능

---

# 작업 보고서: RAG Q&A (Step 7) 구현 (2026-05-27)

## 1. 개요
pgvector 시맨틱 검색 + Redis 시맨틱 캐시 + Claude LLM을 결합한 RAG Q&A 기능을 구현하였습니다. SSE 스트리밍과 비스트리밍 모드를 모두 지원합니다.

## 2. 주요 변경 사항

### 신규 파일

| 파일 | 내용 |
|---|---|
| `app/prompts/qa/system.txt` | Q&A 시스템 프롬프트 |
| `app/prompts/qa/user.jinja2` | Q&A 사용자 프롬프트 (context + question) |
| `app/services/rag_service.py` | `RagService` — 캐시·검색·LLM·캐싱 파이프라인 |

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `pyproject.toml` | `redis[hiredis]>=5.0.0` 추가, mypy override 추가 |
| `app/core/config.py` | `redis_url` 추가 |
| `app/schemas/ai.py` | `QaRequest`, `QaSource`, `QaResponse` 추가 |
| `app/services/claude_service.py` | `generate_qa`, `stream_qa` 메서드 추가 |
| `app/api/deps.py` | `get_redis_client`, `get_rag_service` 추가 |
| `app/api/ai.py` | `POST /ai/qa` 엔드포인트 추가 |

## 3. 핵심 설계 결정

### RAG Q&A 흐름
```
POST /ai/qa
  → OpenAI 임베딩 (1536차원)
  → Redis 시맨틱 캐시 확인 (코사인 유사도 > 0.95 → LLM 호출 스킵)
  → pgvector top-K=5 검색 (threshold=0.7)
  → 컨텍스트 구성 (최대 12000자 ≈ 3000 토큰)
  → Claude 답변 생성 (비스트리밍 or SSE 스트리밍)
  → Redis 캐싱 (TTL 1시간, 최대 100항목)
  → QaResponse 반환
```

### 시맨틱 캐시
- **저장소**: Redis (`rag_cache:{tenant_id}` 키)
- **캐시 구조**: JSON list `[{embedding, answer, sources}]`
- **유사도 계산**: numpy 코사인 유사도 (인메모리)
- **히트 조건**: 코사인 유사도 ≥ 0.95
- **캐시 만료**: TTL 3600초, 최대 100항목 (FIFO 제거)

### SSE 스트리밍
- `stream: true` 파라미터로 활성화
- 캐시 히트 시: 전체 답변을 단일 SSE 이벤트로 반환
- 캐시 미스 시: Claude `messages.stream` API로 청크 단위 yield
- 마지막 이벤트: `data: [DONE]`

### 설계 선택 근거
- **ClaudeService에 `generate_qa`/`stream_qa` 추가**: 기존 `generate_claude_text`를 변경하지 않아 테스트 무결성 유지
- **유사도 계산 위치 (앱 인메모리)**: Redis Vector Search 없이 W3 범위 내 구현; 항목 100개 제한으로 성능 영향 최소화
- **캐시 격리 단위 (tenant_id)**: pgvector 검색과 동일한 격리 경계 사용

## 4. 테스트 결과
```
전체 12 passed (기존 테스트 모두 통과, Step 7 별도 테스트 미작성)
```

## 5. 향후 과제
- Kafka Consumer idempotency를 Redis 기반으로 전환 시 다중 인스턴스 지원
- RAG 캐시 항목 증가 시 Redis Vector Search(RediSearch) 도입 검토


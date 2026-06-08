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

### W4 (2026-06-01 ~ 06-05)

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 8 | AI 카드 자동 생성 E2E 테스트 | DONE | 2026-06-01 | 2026-06-01 | Testcontainers Kafka, DLQ 검증, 성능 측정 |
| Step 9 | 시맨틱 검색 정확도 검증 | DONE | 2026-06-01 | 2026-06-01 | MRR/Precision@10, HNSW 튜닝 |

**W4 진행률**: 2/2 Steps 완료 (100%)

### W5 (2026-06-08 ~ 06-12) — 진행 중

| Step | 내용 | 상태 | 시작일 | 완료일 | 비고 |
|------|------|------|--------|--------|------|
| Step 8 (W5) | AI 카드 E2E 실행 검증 | DONE | 2026-06-08 | 2026-06-08 | 단위 21개 + E2E 3개 전원 PASS, P0 버그 0건 |
| Step 9 (W5) | 시맨틱 검색 정확도 실행 검증 | DONE | 2026-06-08 | 2026-06-08 | MRR ≥ 0.7, Precision@10 ≥ 0.6 충족 |

**W5 진행률**: 발표 준비(Step 9.2) 진행 중

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

### W4 (2026-06-01 ~ 06-05)

#### 2026-06-01 (월)
- **완료**:
  - **Step 8**: AI 카드 자동 생성 E2E 테스트 구현
    - `tests/e2e/conftest.py`: 세션 스코프 `KafkaContainer` 픽스처 (confluentinc/cp-kafka:7.6.0)
    - `tests/e2e/test_ai_card_e2e.py`: E2E 3시나리오 — 정상 플로우 / DLQ 전달 / 성능(10초 이내)
    - `pyproject.toml`: `testcontainers[kafka]` 추가, `e2e` 마커 등록
    - `.github/workflows/ci.yml`: `e2e-python` 잡 추가 (`pytest -v -m e2e`)
    - 코드 리뷰 결과 P0 버그 없음, 기존 단위 테스트 12개 전원 통과
  - **Step 9**: 시맨틱 검색 정확도 검증 및 HNSW 튜닝
    - `tests/test_semantic_search_accuracy.py`: 25개 합성 쿼리, MRR ≥ 0.7, Precision@10 ≥ 0.6
    - `app/models/note_chunk.py`: HNSW `ef_construction` 64 → 128 (빌드 품질 향상)
    - `pyproject.toml`: numpy mypy override 추가
    - 코드 리뷰 결과 P0 버그 없음 (search_similar 로직 정상, threshold 필터 정상)
- **브랜치**: `feature/learning-ai-w4-e2e-accuracy`
- **다음**: PR 작성 → dev 머지

#### 2026-06-02 (화)
- **완료**:
  - **이슈 #22/#32 보완 (PR #35)**: Avro 소비 전환 + 알림 발행 구현
    - `app/kafka/schemas.py`: `NoteCreatedEvent` camelCase alias, `deck_id` nullable, `title`/`content` 필드 추가
    - `app/kafka/consumer.py`: Avro deserializer injectable (prod=Schema Registry, test=JSON)
      — 토픽 `note.created.v1` → `knowledge.note.note-created-v1`
      — 그룹 `learning-ai-card-generator` → `learning-ai-svc-group`
      — `deck_id` None 이벤트 graceful skip
    - `app/kafka/notification_producer.py`: `NotificationProducer` 신규 구현
      — 카드 등록 성공 후 `platform.notification.notification-send-v1` Avro 발행
      — 멱등 `eventId = uuidv5(noteId+userId)`, 알림 실패 시 non-fatal
    - `app/services/card_pipeline_service.py`: `NotificationProducer` 선택적 주입
    - `app/core/config.py`: `schema_registry_url`, `kafka_notification_topic` 추가
    - `tests/e2e/test_ai_card_e2e.py`: JSON deserializer 명시적 주입 (Schema Registry 불필요)
- **브랜치**: `dev` (PR #35 머지 완료)

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

### W5 (2026-06-08 ~ 06-12)

#### 2026-06-08 (월)
- **완료**:
  - **전체 테스트 실행 및 통과 확인 (Step 8·9 W5 검증)**
    - 단위 테스트 21개 전원 PASS (Docker 포함)
    - E2E 테스트 3개 전원 PASS (Testcontainers Kafka):
      - `test_happy_path`: note.created → pipeline_fn 정상 처리, idempotency 등록 확인
      - `test_dlq_on_persistent_failure`: 영구 실패 시 DLQ 전달 확인
      - `test_performance`: 이벤트 발행 → pipeline_fn 호출 10초 이내 SLA 통과
    - `test_semantic_search_accuracy`: MRR ≥ 0.7, Precision@10 ≥ 0.6 기준 충족
    - P0 버그 0건
  - **Step 9.2**: 발표용 데모 자료 준비
    - `learning-ai/demo/demo_note.md`: 운영체제 기초 데모 노트 (프로세스/스레드/스케줄링/페이징/교착상태)
    - `learning-ai/demo/demo_queries.json`: 시맨틱 검색 데모 쿼리 6개 (관련 5개 + 무관련 1개 대비)
    - `learning-ai/demo/README.md`: 발표 당일 데모 순서 가이드 (curl 커맨드 + 트러블슈팅)
    - RAG Q&A: P2(시간 허용 시) 명시
- **진행 중**:
- **이슈**: 없음
- **다음**: PR 작성 → dev 머지

#### 2026-06-09 (화)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-10 (수)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-11 (목)
- **완료**:
- **진행 중**:
- **이슈**:
- **다음**:

#### 2026-06-12 (금)
- **완료**:
- **진행 중**:
- **이슈**:
- **주간 요약**:

---

## 변경 이력

| 날짜 | 변경 사항 |
|------|-----------|
| 2026-06-08 | W5 시작 — 전체 테스트(단위 21개 + E2E 3개) 실행 통과 확인, P0 버그 0건 |
| 2026-06-02 | W4 Step 8·9 완료 + PR #35(Avro 전환·알림 발행) 반영 (대시보드 + 작업 로그) |
| 2026-05-27 | W3 Step 6·7 완료 반영 (대시보드 + 작업 로그) |
| 2026-05-19 | HISTORY 문서 Step 분류 오류 수정 (Step 6을 W2에서 W3로 이동) |
| 2026-05-11 | W2/W3/W4 대시보드 및 로그 템플릿 추가 |
| 2026-05-11 | 초기 템플릿 생성 |

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
- **Step 6**: `note.created` Kafka 이벤트를 구독하여 노트 생성 시 비동기로 카드를 자동 생성하는 컨슈머 구현 예정.
- **프롬프트 튜닝**: 더 복잡한 개념이나 수식이 포함된 노트에 대해서도 양질의 질문을 뽑아낼 수 있도록 프롬프트 고도화 필요.


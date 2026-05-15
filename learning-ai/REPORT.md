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

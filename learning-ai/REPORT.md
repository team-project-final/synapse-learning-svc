# Implementation Report: learning-ai 초기 설정 및 고도화

> 최종 갱신: 2026-05-13 (김나경 가이드 동기화 완료)

본 문서는 `learning-ai` 서비스의 초기 부트스트랩 상태에서 현재의 프로덕션 레디(Production-ready) 구조로 변화하는 과정을 기록한 보고서입니다. 본 프로젝트는 **김나경님의 FastAPI Scaffold 워크플로우 가이드**를 100% 준수하여 리팩토링되었습니다.

## 1. 초기 상태 (Initial State)
- **기준 일자:** 2026-05-13 이전 (부트스트랩 초기 커밋 상태)
- **구조:** 단일 파일 `app/main.py`에 모든 로직이 포함된 단순한 형태.

## 2. 주요 변경 사항 및 작업 이력

### Phase 7: 목킹 전략(Mocking Strategy) 도입 및 테스트 고도화 (2026-05-14)
- **변경 사항:** `00-mocking-strategy.md` 규약에 따른 외부 의존성 격리 및 통합 테스트 환경 구축.
- **외부 API 격리 (`respx`):** `unittest.mock` 기반의 객체 모킹을 폐기하고, HTTP 레이어에서 Anthropic API를 가로채는 `respx` 도입. 실제 네트워크 요청/응답 형식을 검증하고 429 에러 재시도 로직을 정밀하게 테스트함.
- **DB 통합 테스트 (`testcontainers`):** Docker를 활용해 실제 `pgvector` 인스턴스를 띄워 `EmbeddingRepository`를 검증하는 환경 구축.
- **버그 수정:** Anthropic 클라이언트의 내부 중복 재시도 방지를 위해 `max_retries=0` 설정 및 Windows 환경의 비동기 루프 이슈 대응을 위해 테스트 픽스처 최적화.
- **기술적 근거:** 인프라 격리 원칙을 준수하여 로컬 개발 환경에 의존하지 않는 독립적이고 신뢰할 수 있는 테스트 자동화 달성.

## 3. 종합 비교 (Before vs After)

| 구분 | 초기 상태 (Skeleton) | 현재 상태 (Current) |
| :--- | :--- | :--- |
| **설정 파일** | 없음 | `app/core/settings.py` (규격 준수) |
| **ENV Prefix** | 없음 | `LEARNING_AI_` 적용 |
| **API 구조** | `main.py` 내 정의 | `app/api/` 폴더 분리 (Flattened) |
| **응답 형식** | 기본 `dict` | `HealthResponse` Pydantic 모델 |
| **컨테이너화** | 없음 | Docker (8090 포트) |
| **테스트 전략** | 단위 테스트 (Object Mock) | 통합 테스트 (`respx`, `Testcontainers`) |

## 4. 프로젝트 디렉토리 구조도 (Tree)

```text
learning-ai/
├── app/
│   ├── api/
│   │   ├── health.py  # Health 체크 로직
│   │   ├── ai.py      # AI 생성 및 임베딩 로직
│   │   └── deps.py    # 의존성 주입
│   ├── db/
│   │   └── session.py # SQLAlchemy 세션 관리
│   ├── core/
│   │   ├── settings.py   # pydantic-settings (LEARNING_AI_ prefix)
│   │   └── exceptions.py # 전역 예외 처리
│   ├── models/
│   │   └── embedding.py  # SQLAlchemy (pgvector)
│   ├── repositories/
│   │   └── embedding_repository.py
│   ├── schemas/
│   │   ├── health.py  # HealthResponse
│   │   └── ai.py      # AI 관련 DTO
│   ├── services/
│   │   ├── base.py
│   │   ├── claude_service.py
│   │   └── openai_service.py
│   └── main.py        # Entry point (Port 8090)
├── tests/
│   ├── conftest.py    # Testcontainers 및 DB 픽스처
│   ├── test_health.py
│   ├── test_ai.py     # respx 기반 고도화
│   ├── test_embedding_repository.py # 신규 통합 테스트
│   └── __init__.py
├── alembic/           # DB 마이그레이션
├── Dockerfile
├── docker-compose.yml
└── pyproject.toml
```

## 5. 향후 계획 (Next Steps)
- CI/CD 파이프라인 연동 시 Docker Desktop 없이 GitHub Actions 상에서 `testcontainers` 실행 최적화.

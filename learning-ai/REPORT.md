# Implementation Report: learning-ai 초기 설정 및 고도화

> 최종 갱신: 2026-05-13

본 문서는 `learning-ai` 서비스의 초기 부트스트랩 상태에서 현재의 프로덕션 레디(Production-ready) 구조로 변화하는 과정을 기록한 보고서입니다.

## 1. 초기 상태 (Initial State)
- **기준 일자:** 2026-05-13 이전 (부트스트랩 초기 커밋 상태)
- **구조:** 단일 파일 `app/main.py`에 모든 로직이 포함된 단순한 형태.
- **설정:** 앱 이름, 버전 등이 코드 내에 하드코딩됨.
- **기능:** `/health` 엔드포인트 2개만 존재하는 기본 FastAPI 뼈대.
- **결함:** 확장성 부족, 환경 변수 관리 체계 없음, 외부 접근(CORS) 차단, 예외 처리 부재.

## 2. 주요 변경 사항 및 작업 이력

### Phase 1: 환경 변수 및 설정 관리 도입 (2026-05-13)
- **변경 사항:** `pydantic-settings` 라이브러리 도입 및 `app/core/config.py` 생성.
- **추가된 파일:** `app/core/config.py`, `.env.example`.
- **수정된 파일:** `pyproject.toml`, `app/main.py`.
- **기술적 근거:** API 키(OpenAI, Anthropic) 등 민감 정보를 코드와 분리하고, 환경별(dev, prod) 설정을 타입 안정적으로 관리하기 위함.

### Phase 2: 라우터 분리 및 디렉토리 구조화 (2026-05-13)
- **변경 사항:** 기능별 폴더 구조 생성 및 `APIRouter`를 통한 경로 분리.
- **추가된 폴더:** `app/api/v1/endpoints/`, `app/services/`.
- **추가된 파일:** `app/api/v1/api.py`, `app/api/v1/endpoints/health.py`, `app/services/__init__.py`.
- **수정된 파일:** `app/main.py`.
- **기술적 근거:** 향후 AI 기능 확장에 대비하여 `main.py`의 비대화를 방지하고, 관심사 분리(SoC)를 실현하기 위함.

### Phase 3: 보안 및 안정성 강화 (2026-05-13)
- **변경 사항:** CORS 미들웨어 적용 및 전역 예외 처리 시스템 구축.
- **추가된 파일:** `app/core/exceptions.py`.
- **수정된 파일:** `app/main.py`, `app/core/config.py`.
- **기술적 근거:** 프론트엔드 서비스와의 원활한 통신을 허용하고, 어떤 에러 상황에서도 클라이언트에 일관된 형식의 응답을 제공하기 위함.

### Phase 3.8: MSA 아키텍처 정렬 (2026-05-13)
- **변경 사항:** CORS 설정을 개발 환경 전용으로 제한하고, 헤더 기반 인증 목업 도입.
- **추가된 파일:** `app/api/deps.py`.
- **수정된 파일:** `app/main.py`.
- **기술적 근거:** MSA 환경의 API Gateway와 중앙 Auth 서비스 정책에 대응하기 위함. 개별 서비스가 독립적인 인증/CORS 정책을 가지지 않도록 설계함.

## 3. 종합 비교 (Before vs After)

| 구분 | 초기 상태 (Skeleton) | 현재 상태 (Current) |
| :--- | :--- | :--- |
| **인증/인가** | 없음 | Header 기반 Mock (`X-User-ID`) |
| **CORS** | 없음 | 개발 환경 전용 (운영은 Gateway 위임) |
| **파일 구조** | 단일 파일 중심 (`main.py`) | 계층적 구조 (api, core, services) |
| **설정 관리** | 하드코딩 | `pydantic-settings` (.env 연동) |
| **확장성** | 낮음 (파일 수정 시 충돌 위험) | 높음 (기능별 파일 분리 가능) |
| **안정성** | 기본 에러 페이지만 제공 | 전역 예외 처리기로 일관된 응답 |
| **보안** | CORS 설정 없음 | 허용된 도메인 기반 접근 제어 |

## 4. 향후 계획 (Next Steps)
- AI 서비스 연동을 위한 Base Service 구현 및 의존성 주입(DI) 설정.
- OpenAI/Anthropic 클라이언트 통합 및 실제 비즈니스 로직 구현.
- 로깅 시스템 보강 및 테스트 코드 작성.

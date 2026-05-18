# WORKFLOW: @learning-ai-owner — Week 1

> **Task 문서**: [TASK_learning-ai.md](../task/TASK_learning-ai.md)
> **기간**: 2026-05-12 ~ 2026-05-15, 4 영업일
> **기능개발 Workflow**: [README §7](../README.md)

---

## Step 1: FastAPI 프로젝트 초기 설정

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (프로젝트 골격)
- [x] Duration 산정 확인 (0.5일)

### 1.2 요구사항 분석
- [x] FastAPI + uvicorn 프로젝트 구조 분석
- [x] 디렉토리 구조 설계 (app/, app/api/, app/core/, app/services/)
- [x] Python 3.12+ 의존성 목록 도출 (pyproject.toml)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: No (골격만 생성, Health endpoint)
- [x] 권한 종류: 없음
- [x] 공개 API 여부: Yes (/health만 공개)
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계
- [x] 골격 단계 — DB 연동 해당 없음
- [x] 프로젝트 디렉토리 구조도 작성
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토
- [x] 민감 정보 암호화: 비해당 (골격 단계)
- [x] .env.example 작성 (시크릿 플레이스홀더만)
- [x] pydantic-settings 환경변수 관리 확인
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)
- [x] HealthResponse Pydantic 모델 정의 (service, version, status)
- [x] 공통 설정 모델 (Settings) 작성 (pydantic-settings)
- [x] Output Format → TASK 반영

### 1.7 Repository 구현
- [x] 골격 단계 — Repository 해당 없음
- [x] pyproject.toml 기반 프로젝트 초기화 (Poetry 또는 uv)

### 1.8 Service + Test
- [x] FastAPI app 인스턴스 생성
- [x] /health 엔드포인트 구현 (200 OK + 서비스명/버전 응답)
- [x] pytest 설정 및 health 테스트 작성
- [x] 테스트 통과 확인

### 1.9 Controller + Test
- [x] Dockerfile 작성 (multi-stage build)
- [x] docker-compose.yml에 서비스 추가
- [x] uvicorn 실행 확인 (포트 8090)

### 1.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음
- [x] Docker 컨테이너 실행 + /health 응답 확인
- [x] RULE Reference → TASK 반영

**Step 1 Status**: [ ] Not Started / [ ] In Progress / [x] Done

---

## Step 2: Claude API 연동

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (FR-AI-xxx AI 생성)
- [x] Duration 산정 확인 (1.5일)

### 1.2 요구사항 분석
- [x] Anthropic Python SDK 문서 분석
- [x] 에러 핸들링 시나리오 정의 (429 Rate Limit, 500 Internal)
- [x] Exponential backoff 재시도 정책 (1s, 2s, 4s / 최대 3회)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자
- [x] 공개 API 여부: No
- [x] API Key 환경변수 관리 (ANTHROPIC_API_KEY) — 하드코딩 금지
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계
- [x] Claude API 연동 — DB 테이블 해당 없음
- [x] Request/Response 스키마 설계
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토
- [x] 민감 정보 암호화: API Key 환경변수 전용 (코드 노출 금지)
- [x] 사용자 입력 검증 (프롬프트 인젝션 방지)
- [x] 응답 토큰 제한 (최대 4096)
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)
- [x] GenerateRequest Pydantic 모델 정의 (prompt, max_tokens, temperature)
- [x] GenerateResponse Pydantic 모델 정의 (content, model, usage)
- [x] ErrorResponse Pydantic 모델 정의 (detail, code)
- [x] Output Format → TASK 반영

### 1.7 Repository 구현
- [x] DB 연동 없음 — 해당 없음
- [x] `anthropic` Python SDK 의존성 추가 (pyproject.toml)

### 1.8 Service + Test
- [x] ClaudeService 클래스 생성 (비동기 클라이언트)
- [x] 429 Rate Limit 시 exponential backoff 재시도 구현
- [x] 500 에러 시 fallback 응답 처리
- [x] 타임아웃 30초 설정
- [x] pytest mock 테스트 작성 (정상/429/500 시나리오)
- [x] 테스트 통과 확인

### 1.9 Controller + Test
- [x] POST /ai/cards/generate 엔드포인트 구현
- [x] 입력값 검증 (max_tokens 제한 등)
- [x] 에러 핸들러 미들웨어 구현
- [x] pytest 통합 테스트 (mock 기반)
- [x] 테스트 통과 확인

### 1.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] OpenAPI(Swagger) 문서 자동 생성 확인
- [x] RULE Reference → TASK 반영

**Step 2 Status**: [ ] Not Started / [ ] In Progress / [x] Done

---

## Step 3: Embedding API 연동

### 1.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W1 해당 요구사항 확인 (FR-AI-xxx 임베딩)
- [x] Duration 산정 확인 (2일)

### 1.2 요구사항 분석
- [x] OpenAI Embedding API 문서 분석 (text-embedding-3-small)
- [x] 벡터 차원 확인 (1536차원)
- [x] 배치 임베딩 요건 (최대 20건 동시)
- [x] Instructions 초안 → TASK 문서 반영

### 1.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자
- [x] 공개 API 여부: No
- [x] API Key 환경변수 관리 (OPENAI_API_KEY) — 하드코딩 금지
- [x] 결과 → TASK Constraints 반영

### 1.4 ERD 설계 (pgvector 스키마)
- [x] 임베딩은 별도 embeddings 테이블이 아닌 note_chunks.embedding vector(1536) 컬럼에 저장 (ERD 기준)
- [x] pgvector 확장 활성화 (`CREATE EXTENSION vector`)
- [x] 인덱스 설계 (note_chunks.embedding 컬럼에 ivfflat 또는 hnsw 인덱스)
- [x] Alembic 마이그레이션 작성
- [x] Duration(final) 갱신

### 1.5 Security 2차 검토
- [x] 민감 정보 암호화: API Key 환경변수 전용
- [x] 입력 텍스트 길이 검증 (최대 8192 토큰)
- [x] 벡터 저장 시 normalize 적용
- [x] 결과 → TASK Constraints 반영

### 1.6 DTO / Entity 설계 (API First)
- [x] EmbedRequest Pydantic 모델 정의 (texts: List[str])
- [x] EmbedResponse Pydantic 모델 정의 (embeddings: List[List[float]], model)
- [x] Embedding SQLAlchemy 모델 작성 (pgvector Column)
- [x] Output Format → TASK 반영

### 1.7 Repository 구현
- [x] NoteChunkRepository 클래스 작성 (async SQLAlchemy — embedding 컬럼 포함)
- [x] pgvector 확장 + note_chunks 테이블 Alembic 마이그레이션
- [x] `openai` Python SDK 의존성 추가

### 1.8 Service + Test
- [x] EmbeddingService 클래스 생성 (비동기)
- [x] 텍스트 → 1536차원 벡터 변환 로직
- [x] 배치 임베딩 지원 (최대 20개)
- [x] 입력 텍스트 길이 검증
- [x] pytest mock 테스트 작성 (단일/배치/에러 시나리오)
- [x] 테스트 통과 확인

### 1.9 Controller + Test
- [x] POST /ai/embeddings 엔드포인트 구현
- [x] 배치 크기 검증 (최대 20건)
- [x] 에러 핸들링 (OpenAI API 에러)
- [x] pytest 통합 테스트 (mock 기반)
- [x] 테스트 통과 확인

### 1.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] OpenAPI(Swagger) 문서 자동 생성 확인
- [x] RULE Reference → TASK 반영

**Step 3 Status**: [ ] Not Started / [ ] In Progress / [x] Done

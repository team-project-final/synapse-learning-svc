# WORKFLOW: @learning-ai-owner — Week 2

> **Task 문서**: [TASK_learning-ai.md](../task/TASK_learning-ai.md)
> **기간**: 2026-05-18 ~ 2026-05-22, 5 영업일
> **PRD**: [PRD_W2.md](../prd/PRD_W2.md)

---

## Step 4: 시맨틱 검색 골격 — pgvector 임베딩 저장/조회 + 코사인 유사도 검색

### 4.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W2 해당 요구사항 확인 (시맨틱 검색)
- [x] Duration 산정 확인

### 4.2 요구사항 분석
- [x] pgvector 확장 모듈 설정 요건 분석
- [x] 임베딩 벡터 차원 정의 (1536 — OpenAI text-embedding-3-small 기준)
- [x] 코사인 유사도 검색 요건 (top-K, threshold)
- [x] 청크 기반 임베딩 저장 구조 분석
- [x] Instructions 초안 → TASK 문서 반영

### 4.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자 (본인 노트 임베딩만)
- [x] OpenAI API Key 관리: 환경변수 관리, 코드 내 하드코딩 금지
- [x] 결과 → TASK Constraints 반영

### 4.4 ERD 설계
- [x] 임베딩은 별도 chunk_embeddings 테이블이 아닌 note_chunks.embedding vector(1536) 컬럼에 저장 (ERD 기준)
- [x] note_chunks 테이블 확인 (id, note_id, chunk_index, chunk_text, token_count, embedding vector(1536), created_at)
- [x] pgvector 확장 활성화 (CREATE EXTENSION vector)
- [x] 인덱스 설계 (note_chunks.embedding 컬럼에 ivfflat 또는 hnsw 인덱스 — 코사인 유사도)
- [x] Duration(final) 갱신

### 4.5 Security 2차 검토
- [x] 임베딩 데이터 접근 제어 (내부 서비스만)
- [x] OpenAI API 호출 rate limit 관리
- [x] 청크 삭제 시 임베딩 cascade 삭제 확인
- [x] 결과 → TASK Constraints 반영

### 4.6 DTO / Entity 설계 (API First)
- [x] EmbeddingRequest 정의 (note_id, chunk_index, chunk_text)
- [x] SemanticSearchRequest 정의 (query, topK, threshold)
- [x] SemanticSearchResultResponse 정의 (chunk_id, note_id, chunk_text, score)
- [x] NoteChunk Entity 작성 (SQLAlchemy + pgvector — embedding 컬럼 포함)
- [x] Pydantic 모델 작성
- [x] Output Format → TASK 반영

### 4.7 Repository 구현
- [x] NoteChunkRepository 구현 (SQLAlchemy — embedding 컬럼 포함)
- [x] pgvector 코사인 유사도 검색 쿼리 (1 - (embedding <=> query_vector))
- [x] 배치 임베딩 저장 구현 (note_chunks.embedding 갱신)
- [x] Alembic 마이그레이션 스크립트 작성

### 4.8 Service + Test
- [x] EmbeddingService 구현 (generate_embedding, store_embedding, search_similar)
- [x] OpenAI Embedding API 호출 구현 (text-embedding-3-small)
- [x] 코사인 유사도 검색 서비스 (top-K + threshold 필터)
- [x] 배치 임베딩 생성 (청크 목록 → 벡터 목록)
- [x] 단위 테스트 작성 (pytest — OpenAI API mock)
- [x] 유사도 검색 테스트 (고정 벡터 사용)
- [x] 테스트 통과 확인

### 4.9 Controller + Test
- [x] POST /ai/embeddings 엔드포인트 구현 (임베딩 생성)
- [x] POST /ai/search/semantic 엔드포인트 구현 (시맨틱 검색)
- [x] **W2 공통 아키텍처 규정 준수 (ErrorCode L_ 접두사 적용)**
- [x] 401/403 응답 테스트
- [x] 통합 테스트 (임베딩 저장 → 시맨틱 검색 → 결과 검증)
- [x] 테스트 통과 확인

### 4.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] Swagger/OpenAPI 문서 확인
- [x] RULE Reference → TASK 반영

**Step 4 Status**: [ ] Not Started / [ ] In Progress / [x] Done

---

## Step 5: AI 카드 자동 생성 골격 — Note → LLM → Card 목록 생성

### 5.1 TASK 시작
- [x] Step Goal / Done When / Scope / Input 확인
- [x] PRD_W2 해당 요구사항 확인 (AI 카드 자동 생성)
- [x] Duration 산정 확인

### 5.2 요구사항 분석
- [x] Note → LLM → Card 목록 생성 플로우 분석
- [x] LLM 입력 포맷 정의 (노트 내용 + 시스템 프롬프트)
- [x] LLM 출력 포맷 정의 (JSON — front/back 리스트)
- [x] 생성 카드 수 제한 (count)
- [x] 프롬프트 엔지니어링 요건 분석
- [x] Instructions 초안 → TASK 문서 반영

### 5.3 Security 1차 검토
- [x] 인증 필요 여부: Yes (JWT 인증 필요)
- [x] 권한 종류: 로그인 사용자 (본인 노트만)
- [x] LLM API Key 관리: 환경변수 관리, 코드 내 하드코딩 금지
- [x] 사용자 입력 → LLM 전달 시 프롬프트 인젝션 방어
- [x] 결과 → TASK Constraints 반영

### 5.4 ERD 설계
- [x] ai_card_generations 테이블 설계 (id, user_id, note_id, status, card_count, model, prompt_tokens, completion_tokens, created_at)
- [x] 인덱스 설계 (ai_card_generations.user_id, ai_card_generations.note_id)
- [x] Duration(final) 갱신

### 5.5 Security 2차 검토
- [x] LLM 응답 검증 (JSON 파싱 실패 시 처리)
- [x] 생성 카드 내용 최소 검증 (빈 문자열, 과도한 길이)
- [x] LLM API 호출 비용 제한 (사용자별 일일 한도)
- [x] 결과 → TASK Constraints 반영

### 5.6 DTO / Entity 설계 (API First)
- [x] CardGenerateRequest 정의 (noteId, cardType, count, difficulty)
- [x] CardGenerateResponse 정의 (generationId, cards[{front, back}], status)
- [x] GeneratedCardDto 정의 (front, back)
- [x] AiCardGeneration Entity 작성 (SQLAlchemy)
- [x] GenerationStatus Enum 작성 (PENDING, COMPLETED, FAILED)
- [x] Pydantic 모델 작성
- [x] Output Format → TASK 반영

### 5.7 Repository 구현
- [x] AiCardGenerationRepository 구현 (SQLAlchemy)
- [x] findByUserIdOrderByCreatedAtDesc 쿼리
- [x] Alembic 마이그레이션 스크립트 작성
- [x] 프롬프트 템플릿 파일 관리 (prompts/ 디렉토리)

### 5.8 Service + Test
- [x] CardGenerationService 구현 (generate_cards, get_generation_status)
- [x] LLM 프롬프트 엔지니어링 구현 (시스템 프롬프트 + 노트 내용)
- [x] LLM 응답 JSON 파싱 로직 구현
- [x] 생성 카드 검증 로직 (front/back 비어있지 않음, 길이 제한)
- [x] 비동기 처리 구현 (생성 요청 → 상태 폴링)
- [x] 프롬프트 인젝션 방어 로직 (입력 sanitization)
- [x] 단위 테스트 작성 (pytest — LLM API mock)
- [x] 프롬프트 변형 테스트 (한국어/영어, 짧은/긴 노트)
- [x] 테스트 통과 확인

### 5.9 Controller + Test
- [x] POST /ai/cards/generate 엔드포인트 구현 (카드 생성 요청)
- [x] GET /ai/cards/generations/{id} 엔드포인트 구현 (상태 조회 — Wiki 추가 예정)
- [x] GET /ai/cards/generations 엔드포인트 구현 (생성 이력)
- [x] 401/403 응답 테스트
- [x] 통합 테스트 (노트 → LLM → 카드 목록 생성 플로우)
- [x] 테스트 통과 확인

### 5.10 View + Test (해당 시)
- [x] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [x] Swagger/OpenAPI 문서 확인
- [x] RULE Reference → TASK 반영

**Step 5 Status**: [ ] Not Started / [ ] In Progress / [x] Done

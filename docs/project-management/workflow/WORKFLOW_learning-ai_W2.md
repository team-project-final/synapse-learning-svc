# WORKFLOW: @learning-ai-owner — Week 2

> **Task 문서**: [TASK_learning-ai.md](../task/TASK_learning-ai.md)
> **기간**: 2026-05-18 ~ 2026-05-22, 5 영업일
> **PRD**: [PRD_W2.md](../prd/PRD_W2.md)

---

## Step 4: 시맨틱 검색 골격 — pgvector 임베딩 저장/조회 + 코사인 유사도 검색

### 4.1 TASK 시작
- [ ] Step Goal / Done When / Scope / Input 확인
- [ ] PRD_W2 해당 요구사항 확인 (시맨틱 검색)
- [ ] Duration 산정 확인

### 4.2 요구사항 분석
- [ ] pgvector 확장 모듈 설정 요건 분석
- [ ] 임베딩 벡터 차원 정의 (1536 — OpenAI text-embedding-3-small 기준)
- [ ] 코사인 유사도 검색 요건 (top-K, threshold)
- [ ] 청크 기반 임베딩 저장 구조 분석
- [ ] Instructions 초안 → TASK 문서 반영

### 4.3 Security 1차 검토
- [ ] 인증 필요 여부: Yes (JWT 인증 필요)
- [ ] 권한 종류: 로그인 사용자 (본인 노트 임베딩만)
- [ ] OpenAI API Key 관리: 환경변수 관리, 코드 내 하드코딩 금지
- [ ] 결과 → TASK Constraints 반영

### 4.4 ERD 설계
- [ ] 임베딩은 별도 chunk_embeddings 테이블이 아닌 note_chunks.embedding vector(1536) 컬럼에 저장 (ERD 기준)
- [ ] note_chunks 테이블 확인 (id, note_id, chunk_index, chunk_text, token_count, embedding vector(1536), created_at)
- [ ] pgvector 확장 활성화 (CREATE EXTENSION vector)
- [ ] 인덱스 설계 (note_chunks.embedding 컬럼에 ivfflat 또는 hnsw 인덱스 — 코사인 유사도)
- [ ] Duration(final) 갱신

### 4.5 Security 2차 검토
- [ ] 임베딩 데이터 접근 제어 (내부 서비스만)
- [ ] OpenAI API 호출 rate limit 관리
- [ ] 청크 삭제 시 임베딩 cascade 삭제 확인
- [ ] 결과 → TASK Constraints 반영

### 4.6 DTO / Entity 설계 (API First)
- [ ] EmbeddingRequest 정의 (note_id, chunk_index, chunk_text)
- [ ] SemanticSearchRequest 정의 (query, topK, threshold)
- [ ] SemanticSearchResultResponse 정의 (chunk_id, note_id, chunk_text, score)
- [ ] NoteChunk Entity 작성 (SQLAlchemy + pgvector — embedding 컬럼 포함)
- [ ] Pydantic 모델 작성
- [ ] Output Format → TASK 반영

### 4.7 Repository 구현
- [ ] NoteChunkRepository 구현 (SQLAlchemy — embedding 컬럼 포함)
- [ ] pgvector 코사인 유사도 검색 쿼리 (1 - (embedding <=> query_vector))
- [ ] 배치 임베딩 저장 구현 (note_chunks.embedding 갱신)
- [ ] Alembic 마이그레이션 스크립트 작성

### 4.8 Service + Test
- [ ] EmbeddingService 구현 (generate_embedding, store_embedding, search_similar)
- [ ] OpenAI Embedding API 호출 구현 (text-embedding-3-small)
- [ ] 코사인 유사도 검색 서비스 (top-K + threshold 필터)
- [ ] 배치 임베딩 생성 (청크 목록 → 벡터 목록)
- [ ] 단위 테스트 작성 (pytest — OpenAI API mock)
- [ ] 유사도 검색 테스트 (고정 벡터 사용)
- [ ] 테스트 통과 확인

### 4.9 Controller + Test
- [ ] POST /ai/embeddings 엔드포인트 구현 (임베딩 생성)
- [ ] POST /ai/search/semantic 엔드포인트 구현 (시맨틱 검색)
- [ ] 401/403 응답 테스트
- [ ] 통합 테스트 (임베딩 저장 → 시맨틱 검색 → 결과 검증)
- [ ] 테스트 통과 확인

### 4.10 View + Test (해당 시)
- [ ] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [ ] Swagger/OpenAPI 문서 확인
- [ ] RULE Reference → TASK 반영

**Step 4 Status**: [ ] Not Started / [ ] In Progress / [ ] Done

---

## Step 5: AI 카드 자동 생성 골격 — Note → LLM → Card 목록 생성

### 5.1 TASK 시작
- [ ] Step Goal / Done When / Scope / Input 확인
- [ ] PRD_W2 해당 요구사항 확인 (AI 카드 자동 생성)
- [ ] Duration 산정 확인

### 5.2 요구사항 분석
- [ ] Note → LLM → Card 목록 생성 플로우 분석
- [ ] LLM 입력 포맷 정의 (노트 내용 + 시스템 프롬프트)
- [ ] LLM 출력 포맷 정의 (JSON — front/back 리스트)
- [ ] 생성 카드 수 제한 (count)
- [ ] 프롬프트 엔지니어링 요건 분석
- [ ] Instructions 초안 → TASK 문서 반영

### 5.3 Security 1차 검토
- [ ] 인증 필요 여부: Yes (JWT 인증 필요)
- [ ] 권한 종류: 로그인 사용자 (본인 노트만)
- [ ] LLM API Key 관리: 환경변수 관리, 코드 내 하드코딩 금지
- [ ] 사용자 입력 → LLM 전달 시 프롬프트 인젝션 방어
- [ ] 결과 → TASK Constraints 반영

### 5.4 ERD 설계
- [ ] ai_card_generations 테이블 설계 (id, user_id, note_id, status, card_count, model, prompt_tokens, completion_tokens, created_at)
- [ ] 인덱스 설계 (ai_card_generations.user_id, ai_card_generations.note_id)
- [ ] Duration(final) 갱신

### 5.5 Security 2차 검토
- [ ] LLM 응답 검증 (JSON 파싱 실패 시 처리)
- [ ] 생성 카드 내용 최소 검증 (빈 문자열, 과도한 길이)
- [ ] LLM API 호출 비용 제한 (사용자별 일일 한도)
- [ ] 결과 → TASK Constraints 반영

### 5.6 DTO / Entity 설계 (API First)
- [ ] CardGenerateRequest 정의 (noteId, cardType, count, difficulty)
- [ ] CardGenerateResponse 정의 (generationId, cards[{front, back}], status)
- [ ] GeneratedCardDto 정의 (front, back)
- [ ] AiCardGeneration Entity 작성 (SQLAlchemy)
- [ ] GenerationStatus Enum 작성 (PENDING, COMPLETED, FAILED)
- [ ] Pydantic 모델 작성
- [ ] Output Format → TASK 반영

### 5.7 Repository 구현
- [ ] AiCardGenerationRepository 구현 (SQLAlchemy)
- [ ] findByUserIdOrderByCreatedAtDesc 쿼리
- [ ] Alembic 마이그레이션 스크립트 작성
- [ ] 프롬프트 템플릿 파일 관리 (prompts/ 디렉토리)

### 5.8 Service + Test
- [ ] CardGenerationService 구현 (generate_cards, get_generation_status)
- [ ] LLM 프롬프트 엔지니어링 구현 (시스템 프롬프트 + 노트 내용)
- [ ] LLM 응답 JSON 파싱 로직 구현
- [ ] 생성 카드 검증 로직 (front/back 비어있지 않음, 길이 제한)
- [ ] 비동기 처리 구현 (생성 요청 → 상태 폴링)
- [ ] 프롬프트 인젝션 방어 로직 (입력 sanitization)
- [ ] 단위 테스트 작성 (pytest — LLM API mock)
- [ ] 프롬프트 변형 테스트 (한국어/영어, 짧은/긴 노트)
- [ ] 테스트 통과 확인

### 5.9 Controller + Test
- [ ] POST /ai/cards/generate 엔드포인트 구현 (카드 생성 요청)
- [ ] GET /ai/cards/generations/{id} 엔드포인트 구현 (상태 조회 — Wiki 추가 예정)
- [ ] GET /ai/cards/generations 엔드포인트 구현 (생성 이력)
- [ ] 401/403 응답 테스트
- [ ] 통합 테스트 (노트 → LLM → 카드 목록 생성 플로우)
- [ ] 테스트 통과 확인

### 5.10 View + Test (해당 시)
- [ ] Flutter 화면 연동: 해당 없음 (프론트 별도)
- [ ] Swagger/OpenAPI 문서 확인
- [ ] RULE Reference → TASK 반영

**Step 5 Status**: [ ] Not Started / [ ] In Progress / [ ] Done

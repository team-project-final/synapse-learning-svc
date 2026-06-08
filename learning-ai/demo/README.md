# learning-ai 발표 데모 가이드

> **발표일**: 2026-06-15 (월)  
> **서비스 포트**: 8090  
> **데모 소요 시간**: 약 5분

---

## 사전 체크리스트

- [ ] `docker compose up -d` — PostgreSQL(pgvector), Redis, Kafka 기동 확인
- [ ] `uvicorn app.main:app --port 8090` 또는 Docker 컨테이너 기동
- [ ] `GET http://localhost:8090/health` → `{"status":"ok"}` 확인
- [ ] `.venv/Scripts/python demo/seed_demo_data.py` — 데모 노트 청크 DB 삽입 (시맨틱 검색 전 필수)
- [ ] `demo_note.md` 내용을 클립보드에 복사해 두기

---

## 데모 순서

### 1단계 — AI 카드 자동 생성 (약 2분)

`demo_note.md`의 내용을 노트로 사용해 AI 플래시카드를 생성한다.

```bash
curl -s -X POST http://localhost:8090/ai/cards/generate \
  -H "Content-Type: application/json" \
  -H "X-User-Id: demo-user-001" \
  -d '{
    "prompt": "운영체제 기초 — 프로세스, 스레드, 메모리 관리\n\n프로세스(Process)는 실행 중인 프로그램의 인스턴스다. 운영체제는 각 프로세스에 독립된 주소 공간, 파일 핸들, PCB를 할당한다...",
    "task": "card_generation",
    "max_tokens": 1024,
    "temperature": 1.0
  }' | python -m json.tool
```

**기대 결과**: 앞면/뒷면 쌍으로 구성된 플래시카드 3~5장 반환

---

### 2단계 — 시맨틱 검색 (약 2분)

`demo_queries.json`의 쿼리로 시맨틱 검색 정확도를 시연한다.

**관련 쿼리 (높은 유사도 예상)**:
```bash
curl -s -X POST http://localhost:8090/ai/search/semantic \
  -H "Content-Type: application/json" \
  -H "X-User-Id: a0000000-0000-0000-0000-000000000001" \
  -d '{
    "query": "CPU 스케줄링 알고리즘의 종류와 특징",
    "tenant_id": "a0000000-0000-0000-0000-000000000001",
    "top_k": 5
  }' | python -m json.tool
```

**무관련 쿼리 (낮은 유사도 대비)**:
```bash
curl -s -X POST http://localhost:8090/ai/search/semantic \
  -H "Content-Type: application/json" \
  -H "X-User-Id: a0000000-0000-0000-0000-000000000001" \
  -d '{
    "query": "파이썬 판다스 데이터프레임 병합 방법",
    "tenant_id": "a0000000-0000-0000-0000-000000000001",
    "top_k": 5
  }' | python -m json.tool
```

**기대 결과**: 관련 쿼리는 유사도 0.55~0.70 결과 반환, 무관련 쿼리는 빈 결과 (hits=0)

---

### 3단계 — RAG Q&A (시간 허용 시)

> **상태**: P2 (시간 허용 시 시연)  
> 구현 완료(`POST /ai/qa`)되어 있으나 발표 시간 부족 시 생략 가능.  
> 시연할 경우 1단계에서 노트 임베딩이 완료된 후 진행해야 한다.

```bash
curl -s -X POST http://localhost:8090/ai/qa \
  -H "Content-Type: application/json" \
  -H "X-User-Id: demo-user-001" \
  -d '{
    "question": "페이지 폴트가 발생했을 때 운영체제의 처리 과정을 설명해줘",
    "tenant_id": "demo-tenant-001",
    "stream": false
  }' | python -m json.tool
```

**기대 결과**: 노트 기반 답변 + 출처(노트 제목/ID) 포함

---

## 트러블슈팅

| 증상 | 원인 | 조치 |
|------|------|------|
| `connection refused` | 서비스 미기동 | `uvicorn` 또는 Docker 재시작 |
| `L_INTERNAL_ERROR` | API 키 미설정 | `.env`에 `ANTHROPIC_API_KEY`, `OPENAI_API_KEY` 확인 |
| 검색 결과 0건 | 임베딩 미저장 | `POST /ai/embeddings`로 노트 먼저 임베딩 |
| Kafka 연결 오류 | `kafka_enabled=false` | 카드 생성 API 직접 호출로 대체 시연 |

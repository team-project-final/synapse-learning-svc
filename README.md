# synapse-learning-svc

Synapse 플랫폼의 학습 도메인 모노레포입니다. 플래시카드 SRS(간격반복학습) 엔진과 AI 카드 생성·검색 기능을 담당하는 두 서비스로 구성됩니다.

| 서비스 | 언어 | 포트 | 역할 |
|--------|------|------|------|
| `learning-card` | Java 21 / Spring Boot 4 | 8084 | 카드·덱 관리, SRS 복습 스케줄링, 복습 통계 |
| `learning-ai` | Python 3.12 / FastAPI | 8090 | AI 카드 자동 생성, 시맨틱 검색, RAG Q&A |

---

## 아키텍처

```
knowledge-svc ──(note-created-v1)──▶ learning-ai ──(card API)──▶ learning-card
                                          │
                                          └──(notification-send-v1)──▶ platform-svc
```

- **이벤트 드리븐**: Kafka + Avro Schema Registry 기반 비동기 통신
- **토픽 네이밍**: `{ENV_PREFIX}{service}.{domain}.{event}-v{N}` (예: `dev.knowledge.note.note-created-v1`)
- **DLQ**: 3회 지수 백오프 재시도 후 `{topic}.dlq` 격리
- **멀티테넌트**: CloudEvents envelope `tenantId` + JWT 기반 격리

---

## learning-card (Java)

### 기술 스택
- Spring Boot 4 · Spring Modulith · Spring Security (OAuth2 Resource Server)
- JPA/Hibernate · Flyway · PostgreSQL
- Spring Kafka · Confluent Avro Serializer
- Redis (캐시) · ShedLock (분산 스케줄러)
- Actuator · Prometheus

### 주요 기능
- 덱(Deck) · 카드(FlashCard) CRUD
- SM-2 알고리즘 기반 복습 스케줄링
- 복습 세션 관리 및 통계 (일별/주별 히트맵, 기억 유지율)
- `learning.card.review-completed-v1`, `learning.card.review-due-v1` 이벤트 발행

### 로컬 실행
```bash
# 인프라 (Kafka, Schema Registry, PostgreSQL, Redis)
docker compose up -d

# 서버 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
# → http://localhost:8084
# → http://localhost:8084/swagger-ui.html
```

### 주요 환경 변수
| 변수 | 기본값 | 설명 |
|------|--------|------|
| `KAFKA_ENABLED` | `false` | Kafka 이벤트 발행 활성화 |
| `KAFKA_TOPIC_PREFIX` | `""` | 토픽 환경 프리픽스 (예: `dev.`) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `SCHEMA_REGISTRY_URL` | `http://localhost:8086` | Confluent Schema Registry |
| `DB_URL` | `jdbc:postgresql://localhost:5432/synapse_learning` | PostgreSQL URL |
| `REDIS_HOST` | `localhost` | Redis 호스트 |
| `JWT_PUBLIC_KEY` | — | JWT 검증용 RSA 공개키 |

---

## learning-ai (Python)

### 기술 스택
- FastAPI · Uvicorn · Pydantic-Settings
- Anthropic Claude (카드 생성) · OpenAI (임베딩)
- SQLAlchemy 2.0 async · asyncpg · pgvector
- aiokafka · confluent-kafka[avro]
- Redis · Prometheus FastAPI Instrumentator

### 주요 기능
- `knowledge.note.note-created-v1` 구독 → 노트 청킹 → AI 카드 자동 생성
- OpenAI 임베딩 + pgvector 기반 시맨틱 검색
- Claude 기반 RAG Q&A
- AI 카드 생성 완료 시 `platform.notification.notification-send-v1` 발행

### 로컬 실행
```bash
cd learning-ai

# 인프라 (PostgreSQL pgvector, Redis)
docker compose up -d db redis

# 의존성 설치
pip install -e ".[dev]"

# 환경 변수 설정
cp .env.example .env   # API 키 등 입력

# 서버 실행
uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload
# → http://localhost:8090/docs
# → http://localhost:8090/metrics
```

또는 전체 Docker 실행:
```bash
docker compose up --build
```

### 주요 환경 변수 (prefix: `LEARNING_AI_`)
| 변수 | 기본값 | 설명 |
|------|--------|------|
| `LEARNING_AI_ANTHROPIC_API_KEY` | — | Claude API 키 (필수) |
| `LEARNING_AI_OPENAI_API_KEY` | — | OpenAI 임베딩 키 (필수) |
| `LEARNING_AI_KAFKA_ENABLED` | `true` | Kafka 컨슈머 활성화 |
| `LEARNING_AI_KAFKA_TOPIC_PREFIX` | `""` | 토픽 환경 프리픽스 (예: `dev.`) |
| `LEARNING_AI_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `LEARNING_AI_DATABASE_URL` | `postgresql+asyncpg://...` | PostgreSQL URL |
| `LEARNING_AI_SCHEMA_REGISTRY_URL` | `http://localhost:8086` | Schema Registry |

---

## Kafka 토픽

| 토픽 | 방향 | 서비스 |
|------|------|--------|
| `knowledge.note.note-created-v1` | 소비 | learning-ai |
| `learning.card.review-completed-v1` | 발행 | learning-card |
| `learning.card.review-due-v1` | 발행 | learning-card |
| `platform.notification.notification-send-v1` | 발행 | learning-ai |

> 환경 프리픽스(`KAFKA_TOPIC_PREFIX` / `LEARNING_AI_KAFKA_TOPIC_PREFIX`) 설정 시 모든 토픽에 자동 적용됩니다.  
> 미설정 시 빈 문자열로 폴백 (로컬/테스트 하위호환).

---

## 브랜치 전략

- 기능 개발: `feat/<name>` → `dev` → `main`
- 버그 수정: `fix/<name>` → `main` (직접)
- `main`에 직접 커밋 금지

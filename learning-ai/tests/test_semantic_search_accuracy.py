"""시맨틱 검색 정확도 벤치마크 — Step 9

목표:
  - MRR >= 0.7
  - Precision@10 >= 0.6

방법:
  - numpy 합성 임베딩(seed=42)으로 실제 OpenAI 호출 없이 pgvector 정확도 검증
  - 25개 쿼리, 각 쿼리당 관련 7개 + 비관련 8개 청크 (총 375개)
  - 기존 db_session 픽스처(PostgreSQL Testcontainer) 재사용
"""

import uuid
from dataclasses import dataclass, field

import numpy as np
import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository

NUM_QUERIES = 25
RELEVANT_PER_QUERY = 7
IRRELEVANT_PER_QUERY = 8
DIM = 1536

MRR_TARGET = 0.7
PRECISION_AT_10_TARGET = 0.6

_rng = np.random.default_rng(42)


def _unit(v: np.ndarray) -> np.ndarray:
    return v / np.linalg.norm(v)


def _perturb(v: np.ndarray, scale: float) -> np.ndarray:
    return _unit(v + _rng.normal(0, scale, v.shape))


@dataclass
class _QueryCase:
    query_vec: list[float]
    relevant_note_ids: set[uuid.UUID] = field(default_factory=set)
    all_chunks: list[NoteChunk] = field(default_factory=list)


def _build_cases(tenant_id: uuid.UUID) -> list[_QueryCase]:
    cases: list[_QueryCase] = []
    for _ in range(NUM_QUERIES):
        base = _unit(_rng.standard_normal(DIM))
        case = _QueryCase(query_vec=base.tolist())

        for j in range(RELEVANT_PER_QUERY):
            note_id = uuid.uuid4()
            case.relevant_note_ids.add(note_id)
            case.all_chunks.append(
                NoteChunk(
                    tenant_id=tenant_id,
                    note_id=note_id,
                    chunk_index=j,
                    content=f"relevant-{j}",
                    embedding=_perturb(base, scale=0.05).tolist(),
                    embedding_model="synthetic",
                    chunk_strategy="test",
                )
            )

        for j in range(IRRELEVANT_PER_QUERY):
            case.all_chunks.append(
                NoteChunk(
                    tenant_id=tenant_id,
                    note_id=uuid.uuid4(),
                    chunk_index=j,
                    content=f"irrelevant-{j}",
                    embedding=_unit(_rng.standard_normal(DIM)).tolist(),
                    embedding_model="synthetic",
                    chunk_strategy="test",
                )
            )

        cases.append(case)
    return cases


def _mrr(results_per_query: list[list[tuple[bool, float]]]) -> float:
    rrs: list[float] = []
    for results in results_per_query:
        for rank, (is_relevant, _) in enumerate(results, start=1):
            if is_relevant:
                rrs.append(1.0 / rank)
                break
        else:
            rrs.append(0.0)
    return sum(rrs) / len(rrs)


def _precision_at_k(results_per_query: list[list[tuple[bool, float]]], k: int) -> float:
    ps: list[float] = []
    for results in results_per_query:
        top = results[:k]
        ps.append(sum(1 for is_relevant, _ in top if is_relevant) / k)
    return sum(ps) / len(ps)


@pytest.mark.asyncio
async def test_semantic_search_accuracy(db_session: AsyncSession) -> None:
    """25개 합성 쿼리로 시맨틱 검색 MRR / Precision@10 검증."""
    repo = NoteChunkRepository(db_session)
    tenant_id = uuid.uuid4()
    cases = _build_cases(tenant_id)

    all_chunks = [chunk for case in cases for chunk in case.all_chunks]
    await repo.bulk_save_chunks(all_chunks)

    results_per_query: list[list[tuple[bool, float]]] = []
    for case in cases:
        raw = await repo.search_similar(
            tenant_id=tenant_id,
            query_vector=case.query_vec,
            top_k=10,
            threshold=0.0,  # 순위 품질 측정 — 임계값 미적용
        )
        results_per_query.append(
            [(chunk.note_id in case.relevant_note_ids, score) for chunk, score in raw]
        )

    mrr_score = _mrr(results_per_query)
    p10 = _precision_at_k(results_per_query, k=10)

    print(
        f"\n[정확도 결과] MRR={mrr_score:.3f} (목표≥{MRR_TARGET}),"
        f" Precision@10={p10:.3f} (목표≥{PRECISION_AT_10_TARGET})"
    )

    assert mrr_score >= MRR_TARGET, f"MRR {mrr_score:.3f} < {MRR_TARGET}"
    assert p10 >= PRECISION_AT_10_TARGET, f"Precision@10 {p10:.3f} < {PRECISION_AT_10_TARGET}"

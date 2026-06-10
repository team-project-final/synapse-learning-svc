import json
import logging
import uuid
from collections.abc import AsyncGenerator
from typing import Any

import numpy as np
from redis.asyncio import Redis

from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository
from app.schemas.ai import QaResponse, QaSource
from app.services.ai_service import AIService

logger = logging.getLogger(__name__)

_MAX_CONTEXT_CHARS = 12_000  # ≈ 3000 토큰
_CACHE_THRESHOLD = 0.95
_CACHE_MAX_ITEMS = 100
_CACHE_TTL = 3600


class RagService:
    def __init__(
        self,
        ai_service: AIService,
        repo: NoteChunkRepository,
        redis_client: Redis,
    ) -> None:
        self._ai = ai_service
        self._repo = repo
        self._redis = redis_client

    async def answer(self, *, question: str, tenant_id: uuid.UUID) -> QaResponse:
        query_vector = await self._ai.openai.get_embedding(question)
        cache_key = f"rag_cache:{tenant_id}"

        cached = await self._check_cache(cache_key, query_vector)
        if cached is not None:
            logger.info("Cache hit for tenant=%s", tenant_id)
            return cached

        chunks = await self._repo.search_similar(
            tenant_id=tenant_id, query_vector=query_vector, top_k=5, threshold=0.7
        )

        if not chunks:
            return QaResponse(
                answer="제공된 노트에서 관련 정보를 찾을 수 없습니다.",
                sources=[],
                from_cache=False,
            )

        context = self._build_context(chunks)
        sources = [
            QaSource(chunk_id=c.id, note_id=c.note_id, content=c.content, score=s)
            for c, s in chunks
        ]

        answer_text = await self._ai.claude.generate_qa(context=context, question=question)

        response = QaResponse(answer=answer_text, sources=sources, from_cache=False)
        await self._save_cache(cache_key, query_vector, response)
        return response

    async def answer_stream(
        self, *, question: str, tenant_id: uuid.UUID
    ) -> AsyncGenerator[str, None]:
        query_vector = await self._ai.openai.get_embedding(question)
        cache_key = f"rag_cache:{tenant_id}"

        cached = await self._check_cache(cache_key, query_vector)
        if cached is not None:
            logger.info("Cache hit (stream) for tenant=%s", tenant_id)
            yield f"data: {json.dumps({'text': cached.answer, 'from_cache': True})}\n\n"
            yield "data: [DONE]\n\n"
            return

        chunks = await self._repo.search_similar(
            tenant_id=tenant_id, query_vector=query_vector, top_k=5, threshold=0.7
        )

        if not chunks:
            no_info = "제공된 노트에서 관련 정보를 찾을 수 없습니다."
            yield f"data: {json.dumps({'text': no_info})}\n\n"
            yield "data: [DONE]\n\n"
            return

        context = self._build_context(chunks)
        sources = [
            QaSource(chunk_id=c.id, note_id=c.note_id, content=c.content, score=s)
            for c, s in chunks
        ]

        collected: list[str] = []
        try:
            async for text_chunk in self._ai.claude.stream_qa(context=context, question=question):
                collected.append(text_chunk)
                yield f"data: {json.dumps({'text': text_chunk})}\n\n"
        except Exception as e:
            logger.error("Stream interrupted for tenant=%s: %s", tenant_id, e)
            yield f"data: {json.dumps({'error': 'Stream interrupted'})}\n\n"
            yield "data: [DONE]\n\n"
            return

        full_answer = "".join(collected)
        response = QaResponse(answer=full_answer, sources=sources, from_cache=False)
        try:
            await self._save_cache(cache_key, query_vector, response)
        except Exception as e:
            logger.warning("Cache save failed for tenant=%s: %s", tenant_id, e)

        sources_payload = [s.model_dump(mode="json") for s in sources]
        yield f"data: {json.dumps({'sources': sources_payload, 'done': True})}\n\n"
        yield "data: [DONE]\n\n"

    def _build_context(self, chunks: list[tuple[NoteChunk, float]]) -> str:
        parts: list[str] = []
        total = 0
        for chunk, _ in chunks:
            if total + len(chunk.content) > _MAX_CONTEXT_CHARS:
                break
            parts.append(chunk.content)
            total += len(chunk.content)
        return "\n\n---\n\n".join(parts)

    async def _check_cache(
        self, cache_key: str, query_vector: list[float]
    ) -> QaResponse | None:
        raw = await self._redis.get(cache_key)
        if not raw:
            return None
        try:
            items: list[dict[str, Any]] = json.loads(raw)
        except json.JSONDecodeError:
            return None

        q_arr = np.array(query_vector)
        q_norm = np.linalg.norm(q_arr)
        for item in items:
            emb = np.array(item["embedding"])
            sim = float(np.dot(q_arr, emb) / (q_norm * np.linalg.norm(emb)))
            if sim >= _CACHE_THRESHOLD:
                return QaResponse(
                    answer=item["answer"],
                    sources=[QaSource(**s) for s in item["sources"]],
                    from_cache=True,
                )
        return None

    async def _save_cache(
        self, cache_key: str, query_vector: list[float], response: QaResponse
    ) -> None:
        raw = await self._redis.get(cache_key)
        items: list[dict[str, Any]] = json.loads(raw) if raw else []

        items.append({
            "embedding": query_vector,
            "answer": response.answer,
            "sources": [s.model_dump(mode="json") for s in response.sources],
        })
        if len(items) > _CACHE_MAX_ITEMS:
            items = items[-_CACHE_MAX_ITEMS:]

        await self._redis.set(cache_key, json.dumps(items), ex=_CACHE_TTL)
        logger.debug("Cache saved key=%s (total=%d)", cache_key, len(items))

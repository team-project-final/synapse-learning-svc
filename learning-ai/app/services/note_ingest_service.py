import logging
import uuid

from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository
from app.services.openai_service import OpenAIEmbeddingService

logger = logging.getLogger(__name__)


def _split_by_section(content: str) -> list[str]:
    chunks = [s.strip() for s in content.split("##") if s.strip()]
    return chunks if chunks else [content.strip()]


class NoteIngestService:
    def __init__(self, openai: OpenAIEmbeddingService, repo: NoteChunkRepository) -> None:
        self._openai = openai
        self._repo = repo

    async def ingest(self, *, note_id: str, tenant_id: str, content: str) -> None:
        chunks = _split_by_section(content)
        embed_resp = await self._openai.get_embeddings(chunks)

        note_uuid = uuid.UUID(note_id)
        tenant_uuid = uuid.UUID(tenant_id)

        note_chunks = [
            NoteChunk(
                tenant_id=tenant_uuid,
                note_id=note_uuid,
                chunk_index=i,
                content=chunk,
                embedding=vec,
                embedding_model=embed_resp.model,
                chunk_strategy="section",
            )
            for i, (chunk, vec) in enumerate(zip(chunks, embed_resp.embeddings, strict=False))
        ]

        await self._repo.delete_by_note(note_uuid, tenant_uuid)
        await self._repo.bulk_save_chunks(note_chunks)
        logger.info("note_id=%s ingested chunk_count=%d", note_id, len(note_chunks))

import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.note_chunk import NoteChunk


class NoteChunkRepository:
    """Repository for handling persistence of NoteChunks as per ERD 5.4."""

    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def save_chunk(self, chunk: NoteChunk) -> NoteChunk:
        """Save a single note chunk."""
        self.session.add(chunk)
        await self.session.commit()
        await self.session.refresh(chunk)
        return chunk

    async def get_by_note(self, note_id: uuid.UUID, tenant_id: uuid.UUID) -> list[NoteChunk]:
        """Fetch all chunks for a specific note within a tenant."""
        result = await self.session.execute(
            select(NoteChunk).where(
                NoteChunk.note_id == note_id,
                NoteChunk.tenant_id == tenant_id
            ).order_by(NoteChunk.chunk_index)
        )
        return list(result.scalars().all())

    async def bulk_save_chunks(self, chunks: list[NoteChunk]) -> None:
        """Save multiple chunks at once."""
        self.session.add_all(chunks)
        await self.session.commit()

    async def delete_by_note(self, note_id: uuid.UUID, tenant_id: uuid.UUID) -> None:
        """Delete all chunks for a note (Phase 1-3 policy)."""
        result = await self.session.execute(
            select(NoteChunk).where(
                NoteChunk.note_id == note_id,
                NoteChunk.tenant_id == tenant_id
            )
        )
        for chunk in result.scalars().all():
            await self.session.delete(chunk)
        await self.session.commit()

    async def search_similar(
        self,
        tenant_id: uuid.UUID,
        query_vector: list[float],
        top_k: int = 10,
        threshold: float = 0.7,
    ) -> list[tuple[NoteChunk, float]]:
        """
        Search for similar chunks using cosine similarity.
        Returns a list of tuples (NoteChunk, score).
        Score is calculated as 1 - cosine_distance.
        """
        # Cosine distance operator <=> is mapped to .cosine_distance()
        distance = NoteChunk.embedding.cosine_distance(query_vector)
        score = (1 - distance).label("score")

        query = (
            select(NoteChunk, score)
            .where(NoteChunk.tenant_id == tenant_id)
            .where(score >= threshold)
            .order_by(score.desc())
            .limit(top_k)
        )

        result = await self.session.execute(query)
        # result.all() returns list of Row objects, each containing (NoteChunk, float)
        return [(row[0], row[1]) for row in result.all()]

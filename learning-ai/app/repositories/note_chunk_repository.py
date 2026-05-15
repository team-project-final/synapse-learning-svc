import uuid
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.note_chunk import NoteChunk


class NoteChunkRepository:
    """Repository for handling persistence of NoteChunks as per ERD 5.4."""

    def __init__(self, session: AsyncSession):
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

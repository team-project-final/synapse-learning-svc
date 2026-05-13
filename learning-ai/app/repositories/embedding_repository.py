from typing import Any, List

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.embedding import Embedding


class EmbeddingRepository:
    """Repository for handling persistence of vector embeddings."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def save_embedding(
        self, source_id: str, source_type: str, vector: List[float]
    ) -> Embedding:
        """Save a new embedding to the database."""
        db_embedding = Embedding(
            source_id=source_id, source_type=source_type, vector=vector
        )
        self.session.add(db_embedding)
        await self.session.commit()
        await self.session.refresh(db_embedding)
        return db_embedding

    async def get_by_source(self, source_id: str, source_type: str) -> List[Embedding]:
        """Fetch embeddings by source identifier and type."""
        result = await self.session.execute(
            select(Embedding).where(
                Embedding.source_id == source_id, Embedding.source_type == source_type
            )
        )
        return list(result.scalars().all())

    async def bulk_save_embeddings(self, embeddings: List[Embedding]) -> None:
        """Save multiple embeddings at once."""
        self.session.add_all(embeddings)
        await self.session.commit()

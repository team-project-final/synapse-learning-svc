import uuid
from datetime import UTC, datetime
from typing import Any

from pgvector.sqlalchemy import Vector
from sqlalchemy import Index, JSON
from sqlalchemy.orm import Mapped, mapped_column

from app.db.session import Base


class NoteChunk(Base):
    """
    Model for RAG chunks as per ERD 5.4.
    Stores content and its corresponding vector embedding.
    """

    __tablename__ = "note_chunks"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    tenant_id: Mapped[uuid.UUID] = mapped_column(index=True)
    note_id: Mapped[uuid.UUID] = mapped_column(index=True)
    chunk_index: Mapped[int] = mapped_column()
    content: Mapped[str] = mapped_column()
    # 1536 dimensions for text-embedding-3-small
    embedding: Mapped[list[float]] = mapped_column(Vector(1536))
    embedding_model: Mapped[str] = mapped_column()
    embedding_version: Mapped[int] = mapped_column(default=1)
    chunk_strategy: Mapped[str] = mapped_column()
    tokens: Mapped[int | None] = mapped_column()
    metadata_: Mapped[dict[str, Any]] = mapped_column("metadata", JSON, default={})
    created_at: Mapped[datetime] = mapped_column(
        default=lambda: datetime.now(UTC).replace(tzinfo=None)
    )

    __table_args__ = (
        # Partial index for tenant-isolated HNSW search as per ERD 5.1/5.4
        Index(
            "idx_note_chunks_embedding",
            embedding,
            postgresql_using="hnsw",
            postgresql_with={"m": 16, "ef_construction": 64},
            postgresql_ops={"embedding": "vector_cosine_ops"},
            postgresql_where=(tenant_id.isnot(None)),
        ),
    )

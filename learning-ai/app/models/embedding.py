from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import Index
from sqlalchemy.orm import Mapped, mapped_column

from app.db.session import Base


class Embedding(Base):
    """Model for storing vector embeddings."""

    __tablename__ = "embeddings"

    id: Mapped[int] = mapped_column(primary_key=True)
    source_id: Mapped[str] = mapped_column(index=True)
    source_type: Mapped[str] = mapped_column()
    # 1536 dimensions as per Step 3 requirement
    vector: Mapped[list[float]] = mapped_column(Vector(1536))
    created_at: Mapped[datetime] = mapped_column(default=datetime.utcnow)

    __table_args__ = (
        # Optional: Add HNSW index if needed by the workflow
        # Index("idx_embeddings_vector", vector, postgresql_using="hnsw", postgresql_with={"m": 16, "ef_construction": 64}),
    )

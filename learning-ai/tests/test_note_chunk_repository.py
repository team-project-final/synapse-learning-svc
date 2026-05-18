import uuid
from typing import Any

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository


@pytest.mark.asyncio
async def test_save_and_get_note_chunk(db_session: AsyncSession) -> None:
    """Test saving and retrieving a note chunk with metadata."""
    repo = NoteChunkRepository(db_session)
    tenant_id = uuid.uuid4()
    note_id = uuid.uuid4()

    chunk = NoteChunk(
        tenant_id=tenant_id,
        note_id=note_id,
        chunk_index=0,
        content="Test content",
        embedding=[0.1] * 1536,
        embedding_model="text-embedding-3-small",
        chunk_strategy="fixed",
        metadata_={"source": "test"},
    )

    saved = await repo.save_chunk(chunk)
    assert saved.id is not None

    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 1
    assert fetched[0].content == "Test content"
    assert fetched[0].metadata_ == {"source": "test"}


@pytest.mark.asyncio
async def test_bulk_save_chunks(db_session: AsyncSession) -> None:
    """Test bulk saving multiple chunks."""
    repo = NoteChunkRepository(db_session)
    tenant_id = uuid.uuid4()
    note_id = uuid.uuid4()

    chunks = [
        NoteChunk(
            tenant_id=tenant_id,
            note_id=note_id,
            chunk_index=i,
            content=f"Content {i}",
            embedding=[0.1] * 1536,
            embedding_model="text-embedding-3-small",
            chunk_strategy="fixed",
        )
        for i in range(5)
    ]

    await repo.bulk_save_chunks(chunks)

    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 5
    assert fetched[0].chunk_index == 0
    assert fetched[4].chunk_index == 4


@pytest.mark.asyncio
async def test_delete_by_note(db_session: AsyncSession) -> None:
    """Test deleting all chunks for a specific note."""
    repo = NoteChunkRepository(db_session)
    tenant_id = uuid.uuid4()
    note_id = uuid.uuid4()

    chunk = NoteChunk(
        tenant_id=tenant_id,
        note_id=note_id,
        chunk_index=0,
        content="To be deleted",
        embedding=[0.1] * 1536,
        embedding_model="text-embedding-3-small",
        chunk_strategy="fixed",
    )
    await repo.save_chunk(chunk)

    await repo.delete_by_note(note_id, tenant_id)

    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 0


@pytest.mark.asyncio
async def test_search_similar(db_session: AsyncSession) -> None:
    """Test searching for similar chunks using pgvector."""
    repo = NoteChunkRepository(db_session)
    tenant_id = uuid.uuid4()

    # Create two chunks: one very similar to target, one very different
    # We use simple vectors for testing cosine similarity
    # Note: pgvector's cosine_distance = 1 - (A·B / (|A|*|B|))
    # score = 1 - distance = (A·B / (|A|*|B|))
    target_vector = [0.1] * 1536

    similar_chunk = NoteChunk(
        tenant_id=tenant_id,
        note_id=uuid.uuid4(),
        chunk_index=0,
        content="Similar",
        embedding=[0.1] * 1536,
        embedding_model="test",
        chunk_strategy="test",
    )

    different_chunk = NoteChunk(
        tenant_id=tenant_id,
        note_id=uuid.uuid4(),
        chunk_index=0,
        content="Different",
        embedding=[-0.1] * 1536,
        embedding_model="test",
        chunk_strategy="test",
    )

    await repo.bulk_save_chunks([similar_chunk, different_chunk])

    # 1. Search with high threshold
    results = await repo.search_similar(tenant_id, target_vector, top_k=5, threshold=0.9)
    assert len(results) == 1
    chunk, score = results[0]
    assert chunk.content == "Similar"
    assert score > 0.99

    # 2. Search with low threshold (both should appear)
    results = await repo.search_similar(tenant_id, target_vector, top_k=5, threshold=-1.0)
    assert len(results) == 2
    # Ordered by score descending
    assert results[0][0].content == "Similar"
    assert results[1][0].content == "Different"
    assert results[1][1] < 0.0  # Cosine similarity between opposite vectors is -1

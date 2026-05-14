import pytest

from app.models.embedding import Embedding
from app.repositories.embedding_repository import EmbeddingRepository


@pytest.mark.asyncio
async def test_save_and_get_embedding(db_session):
    """
    Test saving and retrieving an embedding.
    Verifies persistence in a real PostgreSQL container.
    """
    repo = EmbeddingRepository(db_session)
    source_id = "test-note-1"
    source_type = "note"
    # 1536 dimensions as per Step 3 requirement
    vector = [0.1] * 1536

    # Save
    saved = await repo.save_embedding(source_id, source_type, vector)
    assert saved.id is not None
    assert saved.source_id == source_id
    assert len(saved.vector) == 1536

    # Get
    retrieved = await repo.get_by_source(source_id, source_type)
    assert len(retrieved) == 1
    assert retrieved[0].source_id == source_id
    # Compare with a small tolerance if needed, but here they should be exact
    assert list(retrieved[0].vector) == pytest.approx(vector)


@pytest.mark.asyncio
async def test_bulk_save_embeddings(db_session):
    """
    Test bulk saving multiple embeddings.
    """
    repo = EmbeddingRepository(db_session)
    embeddings = [
        Embedding(source_id="note-bulk-1", source_type="note", vector=[0.1] * 1536),
        Embedding(source_id="note-bulk-2", source_type="note", vector=[0.2] * 1536),
    ]

    await repo.bulk_save_embeddings(embeddings)

    # Verify each
    res1 = await repo.get_by_source("note-bulk-1", "note")
    assert len(res1) == 1
    assert res1[0].source_id == "note-bulk-1"

    res2 = await repo.get_by_source("note-bulk-2", "note")
    assert len(res2) == 1
    assert res2[0].source_id == "note-bulk-2"

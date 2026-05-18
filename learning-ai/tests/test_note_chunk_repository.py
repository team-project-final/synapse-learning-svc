import uuid
import pytest
from sqlalchemy import select
from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository

@pytest.mark.asyncio
async def test_save_and_get_note_chunk(db_session):
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
        metadata_={"source": "test"}
    )
    
    saved = await repo.save_chunk(chunk)
    assert saved.id is not None
    
    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 1
    assert fetched[0].content == "Test content"
    assert fetched[0].metadata_ == {"source": "test"}

@pytest.mark.asyncio
async def test_bulk_save_chunks(db_session):
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
            chunk_strategy="fixed"
        ) for i in range(5)
    ]
    
    await repo.bulk_save_chunks(chunks)
    
    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 5
    assert fetched[0].chunk_index == 0
    assert fetched[4].chunk_index == 4

@pytest.mark.asyncio
async def test_delete_by_note(db_session):
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
        chunk_strategy="fixed"
    )
    await repo.save_chunk(chunk)
    
    await repo.delete_by_note(note_id, tenant_id)
    
    fetched = await repo.get_by_note(note_id, tenant_id)
    assert len(fetched) == 0

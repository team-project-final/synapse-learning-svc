"""데모용 시드 데이터 삽입 스크립트.

demo_note.md를 ## 섹션 기준으로 청크 분리 → OpenAI 임베딩 → note_chunks 테이블 저장.

실행:
    .venv/Scripts/python demo/seed_demo_data.py
"""

import asyncio
import pathlib
import sys
import uuid

sys.path.insert(0, str(pathlib.Path(__file__).parent.parent))

from sqlalchemy import text
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core.config import settings
from app.models.note_chunk import NoteChunk
from app.repositories.note_chunk_repository import NoteChunkRepository
from app.services.openai_service import OpenAIEmbeddingService

DEMO_TENANT_ID = uuid.UUID("a0000000-0000-0000-0000-000000000001")
DEMO_NOTE_ID = uuid.UUID("b0000000-0000-0000-0000-000000000001")
NOTE_PATH = pathlib.Path(__file__).parent / "demo_note.md"


def _split_chunks(text: str) -> list[str]:
    return [s.strip() for s in text.split("##") if s.strip()]


async def main() -> None:
    note_text = NOTE_PATH.read_text(encoding="utf-8")
    chunks = _split_chunks(note_text)
    print(f"청크 {len(chunks)}개 분리 완료")

    openai_svc = OpenAIEmbeddingService(api_key=settings.openai_api_key or "")
    embed_resp = await openai_svc.get_embeddings(chunks)
    print(f"임베딩 {len(embed_resp.embeddings)}개 생성 완료 (dim={len(embed_resp.embeddings[0])})")

    engine = create_async_engine(settings.database_url)
    async with engine.begin() as conn:
        await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))

    from app.db.session import Base
    import app.models.note_chunk  # noqa: F401 — register model
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    session_factory = async_sessionmaker(engine, expire_on_commit=False)
    async with session_factory() as session:
        repo = NoteChunkRepository(session)

        note_chunks = [
            NoteChunk(
                tenant_id=DEMO_TENANT_ID,
                note_id=DEMO_NOTE_ID,
                chunk_index=i,
                content=chunk,
                embedding=vec,
                embedding_model=embed_resp.model,
                chunk_strategy="section",
            )
            for i, (chunk, vec) in enumerate(zip(chunks, embed_resp.embeddings))
        ]
        await repo.bulk_save_chunks(note_chunks)
        print(f"note_chunks {len(note_chunks)}개 저장 완료")
        print(f"  tenant_id : {DEMO_TENANT_ID}")
        print(f"  note_id   : {DEMO_NOTE_ID}")

    await engine.dispose()
    print("완료")


if __name__ == "__main__":
    asyncio.run(main())

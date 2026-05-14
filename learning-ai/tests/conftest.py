import pytest
from sqlalchemy import text
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker
from testcontainers.postgres import PostgresContainer

from app.db.session import Base
from app.models.embedding import Embedding  # noqa: F401 (Ensure model is registered)


@pytest.fixture
def postgres_container():
    """
    Start a PostgreSQL container with pgvector support.
    Using function scope to match the loop.
    """
    postgres = PostgresContainer("pgvector/pgvector:pg16")
    postgres.start()
    yield postgres
    postgres.stop()


@pytest.fixture
async def db_engine(postgres_container):
    """
    Create a database engine and initialize the schema.
    """
    db_url = postgres_container.get_connection_url().replace(
        "postgresql+psycopg2://", "postgresql+asyncpg://"
    )
    engine = create_async_engine(db_url)

    async with engine.begin() as conn:
        await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))
        await conn.run_sync(Base.metadata.create_all)

    yield engine
    await engine.dispose()


@pytest.fixture
async def db_session(db_engine):
    """
    Provide a transactional database session for each test.
    """
    async_session = async_sessionmaker(db_engine, expire_on_commit=False)
    async with async_session() as session:
        yield session
        await session.rollback()

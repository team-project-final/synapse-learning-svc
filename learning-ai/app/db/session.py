from datetime import datetime
from typing import Any

from sqlalchemy.ext.asyncio import AsyncAttrs, async_sessionmaker, create_async_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from app.core.settings import settings


class Base(AsyncAttrs, DeclarativeBase):
    """Base class for SQLAlchemy models."""

    pass


engine = create_async_engine(settings.database_url, echo=True)
SessionLocal = async_sessionmaker(autocommit=False, autoflush=False, bind=engine)


async def get_db():
    """Dependency for getting database sessions."""
    async with SessionLocal() as session:
        yield session

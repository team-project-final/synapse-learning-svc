from typing import Annotated

from fastapi import Depends, Header
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.db.session import get_db
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService


async def get_current_user(x_user_id: Annotated[str | None, Header()] = None) -> str:
    """
    Mock dependency for getting the current user from Gateway headers.
    """
    if not x_user_id:
        return "mock_user_123"
    return x_user_id


def get_claude_service() -> ClaudeService:
    """Dependency for getting a ClaudeService instance."""
    return ClaudeService(api_key=settings.anthropic_api_key or "")


def get_embedding_service() -> OpenAIEmbeddingService:
    """Dependency for getting an OpenAIEmbeddingService instance."""
    return OpenAIEmbeddingService(api_key=settings.openai_api_key or "")

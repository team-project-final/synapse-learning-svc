from typing import Annotated

from fastapi import Depends, Header, HTTPException
from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession
from starlette import status

from app.core.config import settings
from app.db.session import get_db
from app.repositories.note_chunk_repository import NoteChunkRepository
from app.services.ai_service import AIService
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService
from app.services.rag_service import RagService


async def get_current_user(x_user_id: Annotated[str | None, Header()] = None) -> str:
    """Mock dependency for getting the current user."""
    if not x_user_id:
        return "mock_user_123"
    return x_user_id


def get_claude_service() -> ClaudeService:
    """Dependency for getting a ClaudeService instance."""
    if not settings.anthropic_api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI disabled: Anthropic API key not configured",
        )
    return ClaudeService(api_key=settings.anthropic_api_key)


def get_embedding_service() -> OpenAIEmbeddingService:
    """Dependency for getting an OpenAIEmbeddingService instance."""
    if not settings.openai_api_key:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI disabled: OpenAI API key not configured",
        )
    return OpenAIEmbeddingService(api_key=settings.openai_api_key)


def get_note_chunk_repository(session: AsyncSession = Depends(get_db)) -> NoteChunkRepository:  # noqa: B008
    """Dependency for getting a NoteChunkRepository instance."""
    return NoteChunkRepository(session)


def get_ai_service(
    claude: ClaudeService = Depends(get_claude_service),  # noqa: B008
    openai: OpenAIEmbeddingService = Depends(get_embedding_service),  # noqa: B008
    repo: NoteChunkRepository = Depends(get_note_chunk_repository),  # noqa: B008
) -> AIService:
    """Dependency for getting an AIService orchestrator."""
    return AIService(claude=claude, openai=openai, repo=repo)


def get_redis_client() -> Redis:
    """Dependency for getting a Redis client."""
    return Redis.from_url(settings.redis_url, decode_responses=True)


def get_rag_service(
    ai_service: AIService = Depends(get_ai_service),  # noqa: B008
    repo: NoteChunkRepository = Depends(get_note_chunk_repository),  # noqa: B008
    redis: Redis = Depends(get_redis_client),  # noqa: B008
) -> RagService:
    """Dependency for getting a RagService instance."""
    return RagService(ai_service=ai_service, repo=repo, redis_client=redis)

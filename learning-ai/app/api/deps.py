from typing import Annotated

from fastapi import Header

from app.core.config import settings
from app.services.anthropic_service import AnthropicService
from app.services.openai_service import OpenAIEmbeddingService


async def get_current_user(x_user_id: Annotated[str | None, Header()] = None) -> str:
    """
    Mock dependency for getting the current user from Gateway headers.

    In a real MSA environment, the API Gateway would handle authentication
    and pass the user's ID via a header like 'X-User-ID'.
    """
    if not x_user_id:
        # Default for local development
        return "mock_user_123"
    return x_user_id


def get_anthropic_service() -> AnthropicService:
    """Dependency for getting an AnthropicService instance."""
    return AnthropicService(api_key=settings.anthropic_api_key or "")


def get_embedding_service() -> OpenAIEmbeddingService:
    """Dependency for getting an OpenAIEmbeddingService instance."""
    return OpenAIEmbeddingService(api_key=settings.openai_api_key or "")

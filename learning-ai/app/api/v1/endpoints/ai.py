from typing import Any

from fastapi import APIRouter, Depends

from app.api.deps import get_anthropic_service, get_embedding_service
from app.services.anthropic_service import AnthropicService
from app.services.openai_service import OpenAIEmbeddingService

router = APIRouter()


@router.post("/chat/claude")
# ... (rest of chat_with_claude)


@router.post("/embedding")
async def create_embedding(
    text: str, service: OpenAIEmbeddingService = Depends(get_embedding_service)  # noqa: B008
) -> dict[str, Any]:
    """Generate vector embedding for the given text."""
    vector = await service.get_embedding(text)
    return {
        "text": text,
        "vector_length": len(vector),
        "vector_preview": vector[:5],  # Show first 5 dimensions as preview
    }

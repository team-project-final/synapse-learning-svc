from typing import Any

from fastapi import APIRouter, Depends

from app.api.deps import get_claude_service, get_current_user, get_embedding_service
from app.schemas.ai import EmbedRequest, EmbedResponse, GenerateRequest, GenerateResponse
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService

router = APIRouter()


@router.post("/generate", response_model=GenerateResponse)
async def generate_ai_text(
    request: GenerateRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: ClaudeService = Depends(get_claude_service),  # noqa: B008
) -> GenerateResponse:
    """Step 9: POST /api/v1/ai/generate"""
    return await service.generate_claude_text(request)


@router.post("/embed", response_model=EmbedResponse)
async def create_embeddings(
    request: EmbedRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: OpenAIEmbeddingService = Depends(get_embedding_service),  # noqa: B008
) -> EmbedResponse:
    """
    Step 9: POST /api/v1/ai/embed endpoint.
    Supports batch up to 20 texts.
    """
    return await service.get_embeddings(request.texts)


# Legacy endpoint for compatibility
@router.post("/embedding", deprecated=True)
async def create_embedding(
    text: str, service: OpenAIEmbeddingService = Depends(get_embedding_service)  # noqa: B008
) -> dict[str, Any]:
    """Old single embedding endpoint."""
    vector = await service.get_embedding(text)
    return {
        "text": text,
        "vector_length": len(vector),
        "vector_preview": vector[:5],
    }

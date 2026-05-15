from typing import Any

from fastapi import APIRouter, Depends

from app.api.deps import get_ai_service, get_current_user, get_embedding_service
from app.schemas.ai import EmbedRequest, EmbedResponse, GenerateRequest, GenerateResponse
from app.schemas.base import ApiResponse
from app.services.ai_service import AIService
from app.services.openai_service import OpenAIEmbeddingService

router = APIRouter()


@router.post("/cards/generate", response_model=ApiResponse[GenerateResponse])
async def generate_ai_text(
    request: GenerateRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: AIService = Depends(get_ai_service),  # noqa: B008
) -> ApiResponse[GenerateResponse]:
    """Step 2 Task: POST /ai/cards/generate"""
    data = await service.generate_text_with_fallback(request)
    return ApiResponse(data=data)


@router.post("/embeddings", response_model=ApiResponse[EmbedResponse])
async def create_embeddings(
    request: EmbedRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: OpenAIEmbeddingService = Depends(get_embedding_service),  # noqa: B008
) -> ApiResponse[EmbedResponse]:
    """
    Step 3 Task: POST /ai/embeddings endpoint.
    Supports batch up to 20 texts.
    """
    data = await service.get_embeddings(request.texts)
    return ApiResponse(data=data)


# Legacy endpoint for compatibility
@router.post("/embedding", deprecated=True, response_model=ApiResponse[dict[str, Any]])
async def create_embedding(
    text: str, service: OpenAIEmbeddingService = Depends(get_embedding_service)  # noqa: B008
) -> ApiResponse[dict[str, Any]]:
    """Old single embedding endpoint."""
    vector = await service.get_embedding(text)
    data = {
        "text": text,
        "vector_length": len(vector),
        "vector_preview": vector[:5],
    }
    return ApiResponse(data=data)

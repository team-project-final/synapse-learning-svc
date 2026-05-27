import uuid
from typing import Any

from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse, StreamingResponse
from starlette.responses import Response

from app.api.deps import get_ai_service, get_current_user, get_embedding_service, get_rag_service
from app.schemas.ai import (
    CardGenerateResponse,
    EmbedRequest,
    EmbedResponse,
    GenerateRequest,
    GenerateResponse,
    QaRequest,
    SemanticSearchRequest,
    SemanticSearchResponse,
)
from app.schemas.base import ApiResponse
from app.services.ai_service import AIService
from app.services.openai_service import OpenAIEmbeddingService
from app.services.rag_service import RagService

router = APIRouter()


@router.post("/cards/generate", response_model=ApiResponse[CardGenerateResponse])
async def generate_flashcards(
    request: GenerateRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: AIService = Depends(get_ai_service),  # noqa: B008
) -> ApiResponse[CardGenerateResponse]:
    """
    Step 5 Task: POST /ai/cards/generate.
    Generates a list of flashcards (front/back) from note content.
    """
    data = await service.generate_cards(request)
    return ApiResponse(data=data)


@router.post("/generate", response_model=ApiResponse[GenerateResponse])
async def generate_ai_text(
    request: GenerateRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: AIService = Depends(get_ai_service),  # noqa: B008
) -> ApiResponse[GenerateResponse]:
    """Standard text generation endpoint (generic)."""
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


@router.post("/search/semantic", response_model=ApiResponse[SemanticSearchResponse])
async def semantic_search(
    request: SemanticSearchRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: AIService = Depends(get_ai_service),  # noqa: B008
) -> ApiResponse[SemanticSearchResponse]:
    """
    Step 4 Task: POST /ai/search/semantic.
    Performs vector similarity search on note chunks.
    """
    try:
        tenant_id = uuid.UUID(current_user)
    except ValueError:
        # Mock tenant for non-UUID user strings
        tenant_id = uuid.UUID("00000000-0000-0000-0000-000000000000")

    data = await service.semantic_search(tenant_id, request)
    return ApiResponse(data=data)


@router.post("/qa")
async def qa(
    request: QaRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    rag: RagService = Depends(get_rag_service),  # noqa: B008
) -> Response:
    """
    Step 7 Task: POST /ai/qa.
    RAG Q&A: 시맨틱 캐시 → pgvector 검색 → LLM 답변 생성.
    stream=true 시 SSE(text/event-stream)로 스트리밍 반환.
    """
    try:
        tenant_id = uuid.UUID(current_user)
    except ValueError:
        tenant_id = uuid.UUID("00000000-0000-0000-0000-000000000000")

    if request.stream:
        return StreamingResponse(
            rag.answer_stream(question=request.question, tenant_id=tenant_id),
            media_type="text/event-stream",
        )

    result = await rag.answer(question=request.question, tenant_id=tenant_id)
    return JSONResponse(content=ApiResponse(data=result).model_dump(mode="json"))


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

from typing import Any

from fastapi import APIRouter, Depends

from app.api.deps import get_claude_service, get_current_user, get_embedding_service
from app.schemas.ai import GenerateRequest, GenerateResponse
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService

router = APIRouter()


@router.post("/generate", response_model=GenerateResponse)
async def generate_ai_text(
    request: GenerateRequest,
    current_user: str = Depends(get_current_user),  # noqa: B008
    service: ClaudeService = Depends(get_claude_service),  # noqa: B008
) -> GenerateResponse:
    """
    Step 9 Requirement: POST /api/v1/ai/generate endpoint.
    Includes input validation (via Pydantic) and security (via get_current_user).
    """
    return await service.generate_claude_text(request)


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


# Legacy endpoint for compatibility (can be removed later)
@router.post("/chat/claude", deprecated=True)
async def chat_with_claude(
    prompt: str, service: ClaudeService = Depends(get_claude_service)  # noqa: B008
) -> dict[str, str]:
    """Old interaction with Claude API (Use /generate instead)."""
    response = await service.generate_text(prompt)
    return {"response": response}

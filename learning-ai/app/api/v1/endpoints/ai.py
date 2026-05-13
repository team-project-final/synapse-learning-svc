from fastapi import APIRouter, Depends

from app.api.deps import get_anthropic_service
from app.services.anthropic_service import AnthropicService

router = APIRouter()


@router.post("/chat/claude")
async def chat_with_claude(
    prompt: str,
    service: AnthropicService = Depends(get_anthropic_service),  # noqa: B008
) -> dict[str, str]:
    """Interact with Claude API."""
    response = await service.generate_text(prompt)
    return {"response": response}

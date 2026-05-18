import logging

from app.schemas.ai import GenerateRequest, GenerateResponse
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService

logger = logging.getLogger(__name__)


class AIService:
    """Orchestrator service for AI features with fallback logic."""

    def __init__(self, claude: ClaudeService, openai: OpenAIEmbeddingService):
        self.claude = claude
        self.openai = openai

    async def generate_text_with_fallback(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Claude, with fallback to OpenAI if Claude fails
        after its internal retries.
        """
        try:
            return await self.claude.generate_claude_text(request)
        except Exception as e:
            logger.warning(f"Claude failed, falling back to OpenAI: {e}")
            try:
                return await self.openai.generate_openai_text(request)
            except Exception as openai_err:
                logger.error(f"OpenAI fallback also failed: {openai_err}")
                # If everything fails, we return a fallback response or re-raise
                # Based on rule 13.6, we should probably raise HTTPException in the router
                # or return a graceful error here.
                raise openai_err

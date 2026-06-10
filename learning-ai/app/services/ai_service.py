import json
import logging
import uuid

from anthropic import AuthenticationError
from pydantic import ValidationError

from app.repositories.note_chunk_repository import NoteChunkRepository
from app.schemas.ai import (
    CardGenerateResponse,
    GeneratedCard,
    GenerateRequest,
    GenerateResponse,
    SemanticSearchRequest,
    SemanticSearchResponse,
    SemanticSearchResult,
)
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService

logger = logging.getLogger(__name__)


class AIService:
    """Orchestrator service for AI features with fallback logic."""

    def __init__(
        self,
        claude: ClaudeService,
        openai: OpenAIEmbeddingService,
        repo: NoteChunkRepository,
    ) -> None:
        self.claude = claude
        self.openai = openai
        self.repo = repo

    async def generate_text_with_fallback(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Claude, with fallback to OpenAI if Claude fails
        after its internal retries.
        """
        try:
            return await self.claude.generate_claude_text(request)
        except AuthenticationError:
            raise
        except Exception as e:
            logger.warning(f"Claude failed, falling back to OpenAI: {e}")
            try:
                return await self.openai.generate_openai_text(request)
            except Exception as openai_err:
                logger.error(f"OpenAI fallback also failed: {openai_err}")
                raise openai_err

    async def semantic_search(
        self, tenant_id: uuid.UUID, request: SemanticSearchRequest
    ) -> SemanticSearchResponse:
        """
        Step 4 Task: Semantic search implementation.
        1. Convert query text to vector using OpenAI.
        2. Search for similar chunks in pgvector via repository.
        3. Format and return results.
        """
        # Step 1: Query embedding
        query_vector = await self.openai.get_embedding(request.query)

        # Step 2: Vector search
        results = await self.repo.search_similar(
            tenant_id=tenant_id,
            query_vector=query_vector,
            top_k=request.top_k,
            threshold=request.threshold,
        )

        # Step 3: Format results
        search_results = [
            SemanticSearchResult(
                chunk_id=chunk.id,
                note_id=chunk.note_id,
                content=chunk.content,
                score=score,
            )
            for chunk, score in results
        ]

        return SemanticSearchResponse(results=search_results)

    async def generate_cards(self, request: GenerateRequest) -> CardGenerateResponse:
        """
        Step 5 Task: AI Flashcard generation.
        1. Set task to 'card_generation' to use specific prompts.
        2. Call LLM to get JSON list of cards.
        3. Parse and validate JSON.
        """
        request.task = "card_generation"
        response = await self.generate_text_with_fallback(request)

        try:
            content = response.content.strip()
            if content.startswith("```"):
                start = content.find("[")
                end = content.rfind("]") + 1
                if start == -1 or end == 0:
                    raise ValueError(f"LLM 응답에 JSON 배열이 없습니다: {content[:200]!r}")
                content = content[start:end]

            cards_data = json.loads(content)
            if not isinstance(cards_data, list):
                raise ValueError("AI response is not a JSON list")

            cards = [GeneratedCard(**card) for card in cards_data]

        except (json.JSONDecodeError, ValueError, ValidationError) as e:
            logger.error(f"Failed to parse generated cards: {e}\nContent: {response.content[:500]}")
            raise ValueError(f"Flashcard parsing failed: {e}") from e

        return CardGenerateResponse(
            cards=cards,
            model=response.model,
            usage=response.usage,
        )

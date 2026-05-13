import numpy as np
from openai import AsyncOpenAI

from app.schemas.ai import EmbedResponse
from app.services.base import BaseEmbeddingService


class OpenAIEmbeddingService(BaseEmbeddingService):
    """Service for interacting with OpenAI Embedding API with batch and normalization."""

    def __init__(self, api_key: str):
        self.client = AsyncOpenAI(api_key=api_key)

    async def get_embedding(self, text: str, **kwargs: Any) -> list[float]:
        """Generate a single normalized embedding."""
        response = await self.get_embeddings([text])
        return response.embeddings[0]

    async def get_embeddings(self, texts: list[str]) -> EmbedResponse:
        """
        Generate batch vector embeddings using OpenAI.
        Step 2: OpenAI text-embedding-3-small (1536 dims)
        Step 5/8: Supports batch (up to 20) and normalizes vectors.
        """
        # Batch size validation is handled by Pydantic (EmbedRequest)
        response = await self.client.embeddings.create(input=texts, model="text-embedding-3-small")

        # Step 5 Requirement: Vector normalization
        normalized_embeddings = [self._normalize(data.embedding) for data in response.data]

        return EmbedResponse(embeddings=normalized_embeddings, model=response.model)

    def _normalize(self, vector: list[float]) -> list[float]:
        """L2 normalization of a vector."""
        v = np.array(vector)
        norm = np.linalg.norm(v)
        if norm > 0:
            return (v / norm).tolist()
        return vector

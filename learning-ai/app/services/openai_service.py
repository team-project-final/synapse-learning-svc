from typing import Any

from openai import AsyncOpenAI

from app.services.base import BaseEmbeddingService


class OpenAIEmbeddingService(BaseEmbeddingService):
    """Service for interacting with OpenAI Embedding API."""

    def __init__(self, api_key: str):
        self.client = AsyncOpenAI(api_key=api_key)

    async def get_embedding(self, text: str, **kwargs: Any) -> list[float]:
        """Generate vector embedding using OpenAI."""
        model = kwargs.get("model", "text-embedding-3-small")
        response = await self.client.embeddings.create(input=[text], model=model)
        return response.data[0].embedding

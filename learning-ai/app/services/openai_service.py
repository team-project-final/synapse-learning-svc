from typing import Any

import httpx
import numpy as np
from openai import APIConnectionError, AsyncOpenAI, RateLimitError
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from app.core.logging import track_tokens
from app.core.prompts import load_system_prompt, render_user_prompt
from app.schemas.ai import EmbedResponse, GenerateRequest, GenerateResponse, UsageInfo
from app.services.base import BaseEmbeddingService


class OpenAIEmbeddingService(BaseEmbeddingService):
    """Service for interacting with OpenAI API with stability and tracking."""

    def __init__(self, api_key: str):
        self.client = AsyncOpenAI(
            api_key=api_key, timeout=httpx.Timeout(30.0, connect=5.0), max_retries=0
        )

    async def get_embedding(self, text: str, **kwargs: Any) -> list[float]:
        """Generate a single normalized embedding."""
        response = await self.get_embeddings([text])
        return response.embeddings[0]

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((RateLimitError, APIConnectionError)),
    )
    async def get_embeddings(self, texts: list[str]) -> EmbedResponse:
        """
        Generate batch vector embeddings using OpenAI.
        Step 2: OpenAI text-embedding-3-small (1536 dims)
        Step 5/8: Supports batch (up to 20) and normalizes vectors.
        """
        response = await self.client.embeddings.create(input=texts, model="text-embedding-3-small")

        # Step 5 Requirement: Vector normalization
        normalized_embeddings = [self._normalize(data.embedding) for data in response.data]

        return EmbedResponse(embeddings=normalized_embeddings, model=response.model)

    @track_tokens
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((RateLimitError, APIConnectionError)),
    )
    async def generate_openai_text(self, request: GenerateRequest) -> GenerateResponse:
        """Generate text using OpenAI (primarily for fallback)."""
        system_prompt = load_system_prompt(request.task)
        user_message = render_user_prompt(request.task, prompt=request.prompt)

        response = await self.client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_message},
            ],
            max_tokens=request.max_tokens,
            temperature=request.temperature,
        )

        usage = response.usage
        input_tokens = usage.prompt_tokens if usage else 0
        output_tokens = usage.completion_tokens if usage else 0

        return GenerateResponse(
            content=response.choices[0].message.content or "",
            model=response.model,
            usage=UsageInfo(
                input_tokens=input_tokens,
                output_tokens=output_tokens,
            ),
        )

    def _normalize(self, vector: list[float]) -> list[float]:
        """L2 normalization of a vector."""
        v = np.array(vector)
        norm: float = float(np.linalg.norm(v))
        if norm > 0:
            return (v / norm).tolist()  # type: ignore
        return vector

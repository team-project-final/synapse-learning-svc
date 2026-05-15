from typing import Any

import httpx
from anthropic import APIConnectionError, AsyncAnthropic, InternalServerError, RateLimitError
from tenacity import (
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from app.core.logging import track_tokens
from app.core.prompts import load_system_prompt, render_user_prompt
from app.schemas.ai import GenerateRequest, GenerateResponse, UsageInfo
from app.services.base import BaseAIService


class ClaudeService(BaseAIService):
    """Service for interacting with Anthropic (Claude) API with retry logic."""

    def __init__(self, api_key: str):
        # Set timeout to 30 seconds as per workflow requirement
        self.client = AsyncAnthropic(
            api_key=api_key, timeout=httpx.Timeout(30.0, connect=5.0), max_retries=0
        )

    async def generate_text(self, prompt: str, **kwargs: Any) -> str:
        """Legacy method for BaseAIService compatibility."""
        request = GenerateRequest(prompt=prompt, **kwargs)
        response = await self.generate_claude_text(request)
        return response.content

    @track_tokens
    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        retry=retry_if_exception_type((RateLimitError, APIConnectionError, InternalServerError)),
    )
    async def generate_claude_text(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Claude with exponential backoff.
        Uses tenacity for retry logic as per project rules.
        """
        # Load and render prompts
        system_prompt = load_system_prompt(request.task)
        user_message = render_user_prompt(request.task, prompt=request.prompt)

        message = await self.client.messages.create(
            model="claude-3-5-sonnet-20240620",
            max_tokens=request.max_tokens,
            temperature=request.temperature,
            system=system_prompt,
            messages=[{"role": "user", "content": user_message}],
        )

        content_text = ""
        if message.content and hasattr(message.content[0], "text"):
            content_text = getattr(message.content[0], "text")

        return GenerateResponse(
            content=content_text,
            model=message.model,
            usage=UsageInfo(
                input_tokens=message.usage.input_tokens,
                output_tokens=message.usage.output_tokens,
            ),
        )

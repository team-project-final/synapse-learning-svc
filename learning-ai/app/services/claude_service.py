import asyncio
from typing import Any

from anthropic import AsyncAnthropic, InternalServerError, RateLimitError

from app.schemas.ai import GenerateRequest, GenerateResponse, UsageInfo
from app.services.base import BaseAIService


class ClaudeService(BaseAIService):
    """Service for interacting with Anthropic (Claude) API with retry logic."""

    def __init__(self, api_key: str):
        # Set timeout to 30 seconds as per workflow requirement
        # Set max_retries to 0 to handle retries manually in generate_claude_text
        self.client = AsyncAnthropic(api_key=api_key, timeout=30.0, max_retries=0)

    async def generate_text(self, prompt: str, **kwargs: Any) -> str:
        """Legacy method for BaseAIService compatibility."""
        request = GenerateRequest(prompt=prompt, **kwargs)
        response = await self.generate_claude_text(request)
        return response.content

    async def generate_claude_text(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Claude with exponential backoff and fallback.
        Retries: 1s, 2s, 4s for 429 and 500 errors.
        """
        retries = [1, 2, 4]
        for i, delay in enumerate(retries + [0]):
            try:
                message = await self.client.messages.create(
                    model="claude-3-5-sonnet-20240620",
                    max_tokens=request.max_tokens,
                    temperature=request.temperature,
                    messages=[{"role": "user", "content": request.prompt}],
                )
                return GenerateResponse(
                    content=message.content[0].text,
                    model=message.model,
                    usage=UsageInfo(
                        input_tokens=message.usage.input_tokens,
                        output_tokens=message.usage.output_tokens,
                    ),
                )
            except (RateLimitError, InternalServerError) as e:
                # If there are retries left, wait and retry
                if i < len(retries):
                    await asyncio.sleep(delay)
                    continue

                # Step 8 Requirement: Fallback response for 500 Internal Server Error
                if isinstance(e, InternalServerError):
                    return GenerateResponse(
                        content="서비스가 일시적으로 원활하지 않습니다. 잠시 후 다시 시도해주세요. (Fallback)",
                        model="fallback",
                        usage=UsageInfo(input_tokens=0, output_tokens=0),
                    )
                # Re-raise if it's not a 500 or we're out of retries for 429
                raise e
        # Should not reach here
        raise Exception("Unexpected end of retry loop")

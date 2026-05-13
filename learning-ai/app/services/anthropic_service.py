from typing import Any

from anthropic import AsyncAnthropic

from app.services.base import BaseAIService


class AnthropicService(BaseAIService):
    """Service for interacting with Anthropic (Claude) API."""

    def __init__(self, api_key: str):
        self.client = AsyncAnthropic(api_key=api_key)

    async def generate_text(self, prompt: str, **kwargs: Any) -> str:
        """Generate text using Claude."""
        model = kwargs.get("model", "claude-3-5-sonnet-20240620")
        message = await self.client.messages.create(
            model=model,
            max_tokens=kwargs.get("max_tokens", 1024),
            messages=[{"role": "user", "content": prompt}],
        )
        return message.content[0].text

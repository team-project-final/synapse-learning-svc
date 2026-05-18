import functools
import logging
from collections.abc import Callable
from datetime import date
from typing import Any, TypeVar

from app.schemas.ai import GenerateResponse

logger = logging.getLogger("llm.cost")

# In production, this should be in Redis. For now, we use a global dict.
_daily_tokens: dict[str, int] = {}
DAILY_LIMIT = 500_000

T = TypeVar("T", bound=Callable[..., Any])


class TokenLimitExceededError(Exception):
    """Raised when daily token limit is exceeded."""

    pass


def track_tokens(func: T) -> T:
    """Decorator to track token usage and enforce daily limits."""

    @functools.wraps(func)
    async def wrapper(*args: Any, **kwargs: Any) -> Any:
        today = date.today().isoformat()
        if _daily_tokens.get(today, 0) >= DAILY_LIMIT:
            raise TokenLimitExceededError(f"Daily token limit {DAILY_LIMIT} exceeded")

        response = await func(*args, **kwargs)

        if isinstance(response, GenerateResponse):
            total = response.usage.input_tokens + response.usage.output_tokens
            _daily_tokens[today] = _daily_tokens.get(today, 0) + total

            logger.info(
                "LLM call track",
                extra={
                    "model": response.model,
                    "input_tokens": response.usage.input_tokens,
                    "output_tokens": response.usage.output_tokens,
                    "daily_total": _daily_tokens[today],
                },
            )
        return response

    return wrapper  # type: ignore

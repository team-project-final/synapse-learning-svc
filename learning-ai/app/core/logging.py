import functools
import logging
from collections.abc import Callable
from datetime import date
from typing import Any

from redis.asyncio import Redis

from app.core.config import settings
from app.schemas.ai import GenerateResponse

logger = logging.getLogger("llm.cost")

DAILY_LIMIT = 500_000
_REDIS_KEY_PREFIX = "llm:daily_tokens"
_redis: Redis | None = None


def _get_redis() -> Redis:
    global _redis
    if _redis is None:
        _redis = Redis.from_url(settings.redis_url, decode_responses=True)
    return _redis


class TokenLimitExceededError(Exception):
    """Raised when daily token limit is exceeded."""


def track_tokens[T: Callable[..., Any]](func: T) -> T:
    """Decorator to track token usage and enforce daily limits."""

    @functools.wraps(func)
    async def wrapper(*args: Any, **kwargs: Any) -> Any:
        redis = _get_redis()
        key = f"{_REDIS_KEY_PREFIX}:{date.today().isoformat()}"

        try:
            if int(await redis.get(key) or 0) >= DAILY_LIMIT:
                raise TokenLimitExceededError(f"Daily token limit {DAILY_LIMIT} exceeded")
        except TokenLimitExceededError:
            raise
        except Exception:
            logger.warning("Redis unavailable; skipping daily token limit check")

        response = await func(*args, **kwargs)

        if isinstance(response, GenerateResponse):
            total = response.usage.input_tokens + response.usage.output_tokens
            try:
                daily_total = await redis.incrby(key, total)
                await redis.expire(key, 172_800)  # 48h TTL — 자정 경계 안전 마진
                logger.info(
                    "LLM call track",
                    extra={
                        "model": response.model,
                        "input_tokens": response.usage.input_tokens,
                        "output_tokens": response.usage.output_tokens,
                        "daily_total": daily_total,
                    },
                )
            except Exception:
                logger.warning("Redis unavailable; token usage not recorded")
        return response

    return wrapper  # type: ignore[return-value]

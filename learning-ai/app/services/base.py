from abc import ABC, abstractmethod
from typing import Any


class BaseAIService(ABC):
    """Abstract base class for AI services."""

    @abstractmethod
    async def generate_text(self, prompt: str, **kwargs: Any) -> str:
        """Generate text using an AI model."""
        pass


class BaseEmbeddingService(ABC):
    """Abstract base class for Embedding services."""

    @abstractmethod
    async def get_embedding(self, text: str, **kwargs: Any) -> list[float]:
        """Generate vector embedding for the given text."""
        pass

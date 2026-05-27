import uuid

from pydantic import BaseModel, Field


class UsageInfo(BaseModel):
    """Token usage information."""

    input_tokens: int
    output_tokens: int


class GenerateRequest(BaseModel):
    """Request schema for AI text generation."""

    prompt: str = Field(..., min_length=1, description="The prompt for AI generation")
    task: str = Field("default", description="The task category for prompt selection")
    max_tokens: int = Field(
        1024, gt=0, le=4096, description="Maximum number of tokens to generate"
    )
    temperature: float = Field(
        1.0, ge=0.0, le=1.0, description="Sampling temperature (0.0 to 1.0)"
    )


class GenerateResponse(BaseModel):
    """Response schema for AI text generation."""

    content: str
    model: str
    usage: UsageInfo


class EmbedRequest(BaseModel):
    """Request schema for batch text embedding."""

    texts: list[str] = Field(..., min_length=1, max_length=20)


class EmbedResponse(BaseModel):
    """Response schema for batch text embedding."""

    embeddings: list[list[float]]
    model: str


class SemanticSearchRequest(BaseModel):
    """Request schema for semantic search."""

    query: str = Field(..., min_length=1, description="The text to search for")
    top_k: int = Field(10, ge=1, le=100, description="Number of results to return")
    threshold: float = Field(
        0.7, ge=0.0, le=1.0, description="Minimum similarity score threshold"
    )


class SemanticSearchResult(BaseModel):
    """Schema for a single semantic search result."""

    chunk_id: uuid.UUID
    note_id: uuid.UUID
    content: str
    score: float


class SemanticSearchResponse(BaseModel):
    """Response schema for semantic search."""

    results: list[SemanticSearchResult]


class GeneratedCard(BaseModel):
    """Schema for a single generated flashcard."""

    front: str = Field(..., min_length=1, max_length=200)
    back: str = Field(..., min_length=1, max_length=500)


class CardGenerateResponse(BaseModel):
    """Response schema for AI flashcard generation."""

    cards: list[GeneratedCard]
    model: str
    usage: UsageInfo


class QaRequest(BaseModel):
    """Request schema for RAG Q&A."""

    question: str = Field(..., min_length=1, max_length=500)
    stream: bool = False


class QaSource(BaseModel):
    """A single source chunk referenced in a Q&A answer."""

    chunk_id: uuid.UUID
    note_id: uuid.UUID
    content: str
    score: float


class QaResponse(BaseModel):
    """Response schema for RAG Q&A."""

    answer: str
    sources: list[QaSource]
    from_cache: bool


class ErrorResponse(BaseModel):
    """Standard error response schema."""

    detail: str
    code: str

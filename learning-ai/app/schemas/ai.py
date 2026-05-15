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


class ErrorResponse(BaseModel):
# ...

    """Standard error response schema."""

    detail: str
    code: str

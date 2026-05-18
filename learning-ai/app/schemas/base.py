from datetime import datetime

from pydantic import BaseModel, Field


class ResponseMeta(BaseModel):
    """Metadata for API responses."""

    timestamp: datetime = Field(default_factory=datetime.utcnow)
    request_id: str | None = None


class ApiResponse[T](BaseModel):
    """Standard success response wrapper."""

    success: bool = True
    data: T
    meta: ResponseMeta = Field(default_factory=ResponseMeta)


class ApiErrorDetail(BaseModel):
    """Details for API errors."""

    code: str
    message: str
    details: list[str] = Field(default_factory=list)


class ApiErrorResponse(BaseModel):
    """Standard error response wrapper."""

    success: bool = False
    error: ApiErrorDetail
    meta: ResponseMeta = Field(default_factory=ResponseMeta)

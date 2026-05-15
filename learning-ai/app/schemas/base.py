from datetime import datetime
from typing import Generic, TypeVar

from pydantic import BaseModel, Field

T = TypeVar("T")


class ResponseMeta(BaseModel):
    """Metadata for API responses."""

    timestamp: datetime = Field(default_factory=datetime.utcnow)
    request_id: str | None = None


class ApiResponse(BaseModel, Generic[T]):
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

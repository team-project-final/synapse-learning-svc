from pydantic import BaseModel


class HealthResponse(BaseModel):
    """Standard health check response model."""

    service: str
    version: str
    status: str

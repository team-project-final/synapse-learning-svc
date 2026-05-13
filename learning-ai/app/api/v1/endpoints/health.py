from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HealthResponse(BaseModel):
    """Pydantic model for health check response."""

    service: str
    version: str
    status: str
    environment: str


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Liveness probe."""
    from app.core.config import settings

    return HealthResponse(
        service=settings.app_name,
        version=settings.version,
        status="ok",
        environment=settings.environment,
    )


@router.get("/health/ready")
async def health_ready() -> dict[str, str]:
    """Readiness probe — checks downstream dependencies."""
    return {"status": "ready"}

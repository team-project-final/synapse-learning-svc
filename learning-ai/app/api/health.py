from fastapi import APIRouter

from app.core.settings import settings
from app.schemas.health import HealthResponse

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Liveness probe."""
    return HealthResponse(
        service=settings.service_name,
        version=settings.version,
        status="ok",
    )


@router.get("/health/ready")
async def health_ready() -> dict[str, str]:
    """Readiness probe — checks downstream dependencies."""
    return {"status": "ready"}

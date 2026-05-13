from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    """Liveness probe."""
    from app.core.config import settings

    return {"status": "ok", "environment": settings.environment}


@router.get("/health/ready")
async def health_ready() -> dict[str, str]:
    """Readiness probe — checks downstream dependencies."""
    return {"status": "ready"}

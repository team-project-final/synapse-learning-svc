"""Learning AI FastAPI application entry point."""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.api.ai import router as ai_router
from app.api.health import router as health_router
from app.core.config import settings
from app.core.exceptions import global_exception_handler, http_exception_handler

app = FastAPI(
    title=settings.service_name,
    description="AI-powered learning features for Synapse",
    version=settings.version,
)

# CORS Setting - Only active in development
if settings.environment == "development" or settings.environment == "local":
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[str(origin) for origin in settings.backend_cors_origins],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

# Exception Handlers
app.add_exception_handler(Exception, global_exception_handler)
app.add_exception_handler(HTTPException, http_exception_handler)  # type: ignore

# Routers
app.include_router(health_router)
app.include_router(ai_router, prefix="/ai")

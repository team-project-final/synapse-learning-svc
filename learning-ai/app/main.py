"""Learning AI FastAPI application entry point."""

import logging
import uuid
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from app.api.ai import router as ai_router
from app.api.health import router as health_router
from app.clients.card_client import CardApiClient
from app.core.config import settings
from app.core.exceptions import (
    global_exception_handler,
    http_exception_handler,
    validation_exception_handler,
)
from app.db.session import SessionLocal
from app.kafka.consumer import AiCardKafkaConsumer
from app.kafka.notification_producer import NotificationProducer
from app.repositories.note_chunk_repository import NoteChunkRepository
from app.services.ai_service import AIService
from app.services.card_pipeline_service import AiCardPipelineService
from app.services.claude_service import ClaudeService
from app.services.note_ingest_service import NoteIngestService
from app.services.openai_service import OpenAIEmbeddingService

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    card_client = CardApiClient(base_url=settings.learning_card_service_url)

    if not settings.ai_enabled:
        logger.warning("ANTHROPIC_API_KEY not set — AI card generation disabled")
    if not settings.openai_enabled:
        logger.warning("OPENAI_API_KEY not set — embeddings/semantic search disabled")

    notification: NotificationProducer | None = None
    consumer: AiCardKafkaConsumer | None = None

    if settings.kafka_enabled:
        async def delete_fn(*, note_id: str, tenant_id: str) -> None:
            async with SessionLocal() as session:
                await NoteChunkRepository(session).delete_by_note(
                    uuid.UUID(note_id), uuid.UUID(tenant_id)
                )

        if settings.ai_enabled and settings.openai_enabled:
            claude = ClaudeService(api_key=settings.anthropic_api_key)  # type: ignore[arg-type]
            openai_svc = OpenAIEmbeddingService(api_key=settings.openai_api_key)  # type: ignore[arg-type]
            notification = NotificationProducer()
            await notification.start()

            async def ingest_fn(*, note_id: str, tenant_id: str, content: str) -> None:
                async with SessionLocal() as session:
                    repo = NoteChunkRepository(session)
                    ingest_svc = NoteIngestService(openai=openai_svc, repo=repo)
                    await ingest_svc.ingest(note_id=note_id, tenant_id=tenant_id, content=content)

            async def pipeline_fn(
                *, note_id: str, user_id: str, tenant_id: str, deck_id: str, content: str | None
            ) -> list[str]:
                if not content:
                    logger.warning("note_id=%s has no content in event, skipping", note_id)
                    return []
                async with SessionLocal() as session:
                    repo = NoteChunkRepository(session)
                    ai_svc = AIService(claude=claude, openai=openai_svc, repo=repo)
                    pipeline = AiCardPipelineService(
                        ai_service=ai_svc,
                        card_client=card_client,
                        notification=notification,
                    )
                    return await pipeline.generate_and_save(
                        note_content=content,
                        deck_id=deck_id,
                        user_id=user_id,
                        tenant_id=tenant_id,
                        note_id=note_id,
                    )

            consumer = AiCardKafkaConsumer(
                pipeline_fn=pipeline_fn, ingest_fn=ingest_fn, delete_fn=delete_fn
            )
        else:
            logger.warning("Kafka enabled but AI keys missing — card generation/ingest disabled")
            consumer = AiCardKafkaConsumer(delete_fn=delete_fn)

        await consumer.start()

    yield

    if consumer is not None:
        await consumer.stop()
    if notification is not None:
        await notification.stop()


app = FastAPI(
    title=settings.service_name,
    description="AI-powered learning features for Synapse",
    version=settings.version,
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app, endpoint="/metrics")

# CORS Setting - Only active in development
if settings.environment == "development" or settings.environment == "local":
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[str(origin) for origin in settings.backend_cors_origins],
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Content-Type", "Authorization", "Accept", "X-User-Id"],
    )

# Exception Handlers
app.add_exception_handler(Exception, global_exception_handler)
app.add_exception_handler(HTTPException, http_exception_handler)  # type: ignore
app.add_exception_handler(RequestValidationError, validation_exception_handler)  # type: ignore

# Routers
app.include_router(health_router)
app.include_router(ai_router, prefix="/ai")

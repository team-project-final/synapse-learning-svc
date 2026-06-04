"""Learning AI FastAPI application entry point."""

from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware

from app.api.ai import router as ai_router
from app.api.health import router as health_router
from app.clients.card_client import CardApiClient
from app.clients.note_client import NoteApiClient
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
from app.services.openai_service import OpenAIEmbeddingService


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    note_client = NoteApiClient(base_url=settings.note_service_url)
    card_client = CardApiClient(base_url=settings.learning_card_service_url)
    claude = ClaudeService(api_key=settings.anthropic_api_key or "")
    openai_svc = OpenAIEmbeddingService(api_key=settings.openai_api_key or "")

    notification: NotificationProducer | None = None
    consumer: AiCardKafkaConsumer | None = None

    if settings.kafka_enabled:
        notification = NotificationProducer()
        await notification.start()

        async def pipeline_fn(
            *, note_id: str, user_id: str, tenant_id: str, deck_id: str
        ) -> list[str]:
            note_content = await note_client.get_note_content(
                note_id=note_id, user_id=user_id, tenant_id=tenant_id
            )
            async with SessionLocal() as session:
                repo = NoteChunkRepository(session)
                ai_svc = AIService(claude=claude, openai=openai_svc, repo=repo)
                pipeline = AiCardPipelineService(
                    ai_service=ai_svc,
                    card_client=card_client,
                    notification=notification,
                )
                return await pipeline.generate_and_save(
                    note_content=note_content,
                    deck_id=deck_id,
                    user_id=user_id,
                    tenant_id=tenant_id,
                    note_id=note_id,
                )

        consumer = AiCardKafkaConsumer(pipeline_fn=pipeline_fn)
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

# CORS Setting - Only active in development
if settings.environment == "development" or settings.environment == "local":
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[str(origin) for origin in settings.backend_cors_origins],
        allow_origin_regex=r"http://localhost:\d+",  # Flutter web 모든 포트 허용
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

# Exception Handlers
app.add_exception_handler(Exception, global_exception_handler)
app.add_exception_handler(HTTPException, http_exception_handler)  # type: ignore
app.add_exception_handler(RequestValidationError, validation_exception_handler)  # type: ignore

# Routers
app.include_router(health_router)
app.include_router(ai_router, prefix="/ai")

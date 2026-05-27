import asyncio
import contextlib
import json
import logging
from typing import Any, Protocol

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from tenacity import AsyncRetrying, stop_after_attempt, wait_exponential

from app.core.config import settings
from app.kafka.schemas import NoteCreatedEvent

logger = logging.getLogger(__name__)


class EventHandlerFn(Protocol):
    async def __call__(
        self, *, note_id: str, user_id: str, tenant_id: str, deck_id: str
    ) -> list[str]: ...


class AiCardKafkaConsumer:
    def __init__(self, pipeline_fn: EventHandlerFn) -> None:
        self._pipeline_fn = pipeline_fn
        self._consumer: AIOKafkaConsumer | None = None
        self._producer: AIOKafkaProducer | None = None
        self._processed: set[str] = set()
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        self._consumer = AIOKafkaConsumer(
            settings.kafka_note_created_topic,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_consumer_group_id,
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
            enable_auto_commit=False,
        )
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        )
        await self._consumer.start()
        await self._producer.start()
        self._task = asyncio.create_task(self._consume_loop())
        logger.info(
            "Kafka consumer started topic=%s group=%s",
            settings.kafka_note_created_topic,
            settings.kafka_consumer_group_id,
        )

    async def stop(self) -> None:
        if self._task:
            self._task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._task
        if self._consumer:
            await self._consumer.stop()
        if self._producer:
            await self._producer.stop()
        logger.info("Kafka consumer stopped")

    async def _consume_loop(self) -> None:
        assert self._consumer is not None
        try:
            async for msg in self._consumer:
                await self._handle_message(msg)
        except asyncio.CancelledError:
            raise
        except Exception:
            logger.exception("Consumer loop crashed")
            raise

    async def _handle_message(self, msg: Any) -> None:
        try:
            event = NoteCreatedEvent.model_validate(msg.value)
        except Exception:
            logger.exception("Invalid event at offset %d → DLQ", msg.offset)
            await self._send_to_dlq(msg.value)
            await self._consumer.commit()  # type: ignore[union-attr]
            return

        if event.event_id in self._processed:
            logger.info("Duplicate event_id=%s skipped", event.event_id)
            await self._consumer.commit()  # type: ignore[union-attr]
            return

        try:
            await asyncio.wait_for(self._process_with_retry(event), timeout=60.0)
            self._processed.add(event.event_id)
            logger.info("Processed event_id=%s note_id=%s", event.event_id, event.note_id)
        except Exception:
            logger.exception("Event %s failed after retries → DLQ", event.event_id)
            await self._send_to_dlq(msg.value)
        finally:
            await self._consumer.commit()  # type: ignore[union-attr]

    async def _process_with_retry(self, event: NoteCreatedEvent) -> None:
        async for attempt in AsyncRetrying(
            stop=stop_after_attempt(3),
            wait=wait_exponential(multiplier=2, max=8),
            reraise=True,
        ):
            with attempt:
                await self._pipeline_fn(
                    note_id=event.note_id,
                    user_id=event.user_id,
                    tenant_id=event.tenant_id,
                    deck_id=event.deck_id,
                )

    async def _send_to_dlq(self, raw: dict[str, Any]) -> None:
        assert self._producer is not None
        await self._producer.send_and_wait(settings.kafka_dlq_topic, value=raw)
        logger.warning("DLQ topic=%s: %s", settings.kafka_dlq_topic, raw)

import asyncio
import contextlib
import json
import logging
from collections import OrderedDict
from collections.abc import Callable
from typing import Any, Protocol

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from confluent_kafka.serialization import MessageField, SerializationContext
from tenacity import AsyncRetrying, stop_after_attempt, wait_exponential

from app.core.config import settings
from app.kafka.schemas import NoteCreatedEvent, NoteDeletedEvent
from app.kafka.ssl_support import kafka_ssl_context

logger = logging.getLogger(__name__)


class EventHandlerFn(Protocol):
    async def __call__(
        self,
        *,
        note_id: str,
        user_id: str,
        tenant_id: str,
        deck_id: str,
        content: str | None,
    ) -> list[str]: ...


class IngestFn(Protocol):
    async def __call__(self, *, note_id: str, tenant_id: str, content: str) -> None: ...


class NoteDeleteFn(Protocol):
    async def __call__(self, *, note_id: str, tenant_id: str) -> None: ...


def _make_avro_deserializer(topic: str) -> Callable[[bytes], Any]:
    sr = SchemaRegistryClient({"url": settings.schema_registry_url})
    avro_deser = AvroDeserializer(schema_registry_client=sr)
    ctx = SerializationContext(topic, MessageField.VALUE)
    return lambda raw: avro_deser(raw, ctx)


_DEDUP_MAX = 10_000


class AiCardKafkaConsumer:
    def __init__(
        self,
        pipeline_fn: EventHandlerFn | None = None,
        ingest_fn: IngestFn | None = None,
        delete_fn: NoteDeleteFn | None = None,
        value_deserializer: Callable[[bytes], Any] | None = None,
    ) -> None:
        self._pipeline_fn = pipeline_fn
        self._ingest_fn = ingest_fn
        self._delete_fn = delete_fn
        self._value_deserializer = value_deserializer
        self._consumer: AIOKafkaConsumer | None = None
        self._producer: AIOKafkaProducer | None = None
        self._processed: OrderedDict[str, None] = OrderedDict()
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        deser = self._value_deserializer or _make_avro_deserializer(
            settings.kafka_note_created_topic
        )
        self._consumer = AIOKafkaConsumer(
            settings.kafka_note_created_topic,
            settings.kafka_note_deleted_topic,
            bootstrap_servers=settings.kafka_bootstrap_servers,
            group_id=settings.kafka_consumer_group_id,
            value_deserializer=deser,
            enable_auto_commit=False,
            security_protocol=settings.kafka_security_protocol,
            ssl_context=kafka_ssl_context(),
        )
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            security_protocol=settings.kafka_security_protocol,
            ssl_context=kafka_ssl_context(),
        )
        await self._consumer.start()
        await self._producer.start()
        self._task = asyncio.create_task(self._consume_loop())
        logger.info(
            "Kafka consumer started topics=%s,%s group=%s",
            settings.kafka_note_created_topic,
            settings.kafka_note_deleted_topic,
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
        if msg.topic == settings.kafka_note_deleted_topic:
            await self._handle_delete_message(msg)
        else:
            await self._handle_create_message(msg)

    async def _handle_create_message(self, msg: Any) -> None:
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
            # 1. Semantic ingest — deck_id 유무 관계없이 항상 실행
            if self._ingest_fn is not None:
                if event.content:
                    try:
                        await asyncio.wait_for(
                            self._ingest_fn(
                                note_id=event.note_id,
                                tenant_id=event.tenant_id,
                                content=event.content,
                            ),
                            timeout=60.0,
                        )
                    except Exception:
                        logger.exception("Semantic ingest failed for note_id=%s", event.note_id)
                else:
                    logger.warning("note_id=%s has no content, skipping ingest", event.note_id)

            # 2. 카드 생성 — deck_id 있을 때만 실행
            if event.deck_id is not None:
                if self._pipeline_fn is not None:
                    await asyncio.wait_for(self._process_with_retry(event), timeout=60.0)
                else:
                    logger.info(
                        "pipeline_fn not configured, skipping card gen event_id=%s",
                        event.event_id,
                    )
            else:
                logger.info("event_id=%s has no deck_id, skipping card generation", event.event_id)

            self._processed[event.event_id] = None
            if len(self._processed) > _DEDUP_MAX:
                self._processed.popitem(last=False)
            logger.info("Processed event_id=%s note_id=%s", event.event_id, event.note_id)
        except Exception:
            logger.exception("Event %s failed after retries → DLQ", event.event_id)
            await self._send_to_dlq(msg.value)
        finally:
            await self._consumer.commit()  # type: ignore[union-attr]

    async def _handle_delete_message(self, msg: Any) -> None:
        try:
            event = NoteDeletedEvent.model_validate(msg.value)
        except Exception:
            logger.exception("Invalid delete event at offset %d → DLQ", msg.offset)
            await self._send_to_dlq(msg.value, topic=settings.kafka_note_deleted_dlq_topic)
            await self._consumer.commit()  # type: ignore[union-attr]
            return

        if event.event_id in self._processed:
            logger.info("Duplicate delete event_id=%s skipped", event.event_id)
            await self._consumer.commit()  # type: ignore[union-attr]
            return

        if self._delete_fn is None:
            logger.warning(
                "delete_fn not configured, skipping chunk deletion event_id=%s",
                event.event_id,
            )
            await self._consumer.commit()  # type: ignore[union-attr]
            return

        try:
            await self._delete_with_retry(event)
            self._processed[event.event_id] = None
            if len(self._processed) > _DEDUP_MAX:
                self._processed.popitem(last=False)
            logger.info("Deleted chunks for event_id=%s note_id=%s", event.event_id, event.note_id)
        except Exception:
            logger.exception("Delete event %s failed after retries → DLQ", event.event_id)
            await self._send_to_dlq(msg.value, topic=settings.kafka_note_deleted_dlq_topic)
        finally:
            await self._consumer.commit()  # type: ignore[union-attr]

    async def _process_with_retry(self, event: NoteCreatedEvent) -> None:
        assert event.deck_id is not None  # noqa: S101 — caller ensures this
        assert self._pipeline_fn is not None  # noqa: S101 — caller ensures this
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
                    content=event.content,
                )

    async def _delete_with_retry(self, event: NoteDeletedEvent) -> None:
        assert self._delete_fn is not None  # noqa: S101 — caller ensures this
        async for attempt in AsyncRetrying(
            stop=stop_after_attempt(3),
            wait=wait_exponential(multiplier=2, max=8),
            reraise=True,
        ):
            with attempt:
                await self._delete_fn(
                    note_id=event.note_id,
                    tenant_id=event.tenant_id,
                )

    async def _send_to_dlq(self, raw: dict[str, Any], topic: str | None = None) -> None:
        assert self._producer is not None
        dlq = topic if topic is not None else settings.kafka_dlq_topic
        await self._producer.send_and_wait(dlq, value=raw)
        logger.warning("DLQ topic=%s: %s", dlq, raw)

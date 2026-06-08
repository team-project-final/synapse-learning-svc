import json
import logging
import ssl
import uuid

from aiokafka import AIOKafkaProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import MessageField, SerializationContext

from app.core.config import settings

logger = logging.getLogger(__name__)

# platform.NotificationSend Avro 스키마 (synapse-shared 미러링)
_NOTIFICATION_SEND_SCHEMA = json.dumps(
    {
        "type": "record",
        "name": "NotificationSend",
        "namespace": "com.synapse.event.platform",
        "fields": [
            {"name": "userId", "type": "string", "default": ""},
            {"name": "tenantId", "type": "string", "default": ""},
            {"name": "notificationType", "type": "string", "default": ""},
            {
                "name": "channels",
                "type": {"type": "array", "items": "string"},
                "default": [],
            },
            {"name": "title", "type": "string", "default": ""},
            {"name": "body", "type": "string", "default": ""},
            {"name": "emailSubject", "type": ["null", "string"], "default": None},
            {"name": "emailHtmlBody", "type": ["null", "string"], "default": None},
            {
                "name": "data",
                "type": {"type": "map", "values": "string"},
                "default": {},
            },
        ],
    }
)


class NotificationProducer:
    """platform.notification.notification-send-v1 Avro 발행자."""

    def __init__(self) -> None:
        sr = SchemaRegistryClient({"url": settings.schema_registry_url})
        self._serializer = AvroSerializer(
            sr, _NOTIFICATION_SEND_SCHEMA, to_dict=lambda obj, ctx: obj
        )
        self._ctx = SerializationContext(settings.kafka_notification_topic, MessageField.VALUE)
        self._producer: AIOKafkaProducer | None = None

    async def start(self) -> None:
        ssl_context = ssl.create_default_context() if settings.kafka_security_protocol == "SSL" else None
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            security_protocol=settings.kafka_security_protocol,
            ssl_context=ssl_context,
        )
        await self._producer.start()
        logger.info("NotificationProducer started topic=%s", settings.kafka_notification_topic)

    async def stop(self) -> None:
        if self._producer:
            await self._producer.stop()
        logger.info("NotificationProducer stopped")

    async def send_ai_cards_ready(
        self, *, note_id: str, user_id: str, tenant_id: str, card_count: int
    ) -> None:
        assert self._producer is not None
        event_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"{note_id}{user_id}"))
        payload = {
            "userId": user_id,
            "tenantId": tenant_id,
            "notificationType": "AI_CARDS_READY",
            "channels": ["FCM"],
            "title": "AI 카드가 생성되었습니다",
            "body": f"노트에서 {card_count}장의 카드가 생성되었습니다.",
            "emailSubject": None,
            "emailHtmlBody": None,
            "data": {"eventId": event_id, "noteId": note_id, "cardCount": str(card_count)},
        }
        avro_bytes: bytes = self._serializer(payload, self._ctx)
        await self._producer.send_and_wait(
            settings.kafka_notification_topic,
            value=avro_bytes,
            key=tenant_id.encode(),
        )
        logger.info(
            "NotificationSend published eventId=%s noteId=%s cardCount=%d",
            event_id,
            note_id,
            card_count,
        )

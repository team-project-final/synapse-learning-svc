"""TDD: MSK TLS 배선 검증 (이슈 #45)."""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest


def test_settings_reads_kafka_security_protocol(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("LEARNING_AI_KAFKA_SECURITY_PROTOCOL", "SSL")
    from app.core.config import Settings

    assert Settings().kafka_security_protocol == "SSL"


def test_settings_default_kafka_security_protocol() -> None:
    from app.core.config import Settings

    assert Settings().kafka_security_protocol == "PLAINTEXT"


@pytest.mark.asyncio
async def test_consumer_passes_security_protocol_to_aiokafka() -> None:
    mock_consumer = MagicMock()
    mock_consumer.start = AsyncMock()
    mock_producer = MagicMock()
    mock_producer.start = AsyncMock()

    from app.core.config import Settings
    ssl_settings = Settings()
    ssl_settings.__dict__["kafka_security_protocol"] = "SSL"

    with (
        patch(
            "app.kafka.consumer.AIOKafkaConsumer", return_value=mock_consumer
        ) as mock_consumer_cls,
        patch(
            "app.kafka.consumer.AIOKafkaProducer", return_value=mock_producer
        ) as mock_producer_cls,
        patch("app.kafka.consumer._make_avro_deserializer", return_value=lambda x: x),
        patch("app.kafka.consumer.settings", ssl_settings),
    ):
        from app.kafka.consumer import AiCardKafkaConsumer
        consumer = AiCardKafkaConsumer(pipeline_fn=AsyncMock())
        await consumer.start()

        _, kwargs = mock_consumer_cls.call_args
        assert kwargs.get("security_protocol") == "SSL"

        _, kwargs = mock_producer_cls.call_args
        assert kwargs.get("security_protocol") == "SSL"


@pytest.mark.asyncio
async def test_notification_producer_passes_security_protocol() -> None:
    mock_producer = MagicMock()
    mock_producer.start = AsyncMock()

    from app.core.config import Settings
    ssl_settings = Settings()
    ssl_settings.__dict__["kafka_security_protocol"] = "SSL"

    with (
        patch(
            "app.kafka.notification_producer.AIOKafkaProducer", return_value=mock_producer
        ) as mock_producer_cls,
        patch("app.kafka.notification_producer.SchemaRegistryClient"),
        patch("app.kafka.notification_producer.AvroSerializer"),
        patch("app.kafka.notification_producer.settings", ssl_settings),
    ):
        from app.kafka.notification_producer import NotificationProducer
        producer = NotificationProducer()
        await producer.start()

        _, kwargs = mock_producer_cls.call_args
        assert kwargs.get("security_protocol") == "SSL"

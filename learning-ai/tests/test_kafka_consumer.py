from typing import Any
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.core.config import settings
from app.kafka.consumer import AiCardKafkaConsumer

VALID_EVENT: dict[str, Any] = {
    "event_id": "evt-0001",
    "note_id": "note-1111",
    "user_id": "user-2222",
    "tenant_id": "tenant-3333",
    "deck_id": "deck-4444",
}


class MockMsg:
    def __init__(self, value: dict[str, Any], offset: int = 0) -> None:
        self.value = value
        self.offset = offset


@pytest.fixture
def mock_pipeline_fn() -> AsyncMock:
    return AsyncMock(return_value=["card-id-1", "card-id-2"])


@pytest.fixture
def consumer(mock_pipeline_fn: AsyncMock) -> AiCardKafkaConsumer:
    c = AiCardKafkaConsumer(pipeline_fn=mock_pipeline_fn)
    c._consumer = MagicMock()
    c._consumer.commit = AsyncMock()
    c._producer = MagicMock()
    c._producer.send_and_wait = AsyncMock()
    return c


async def test_handle_message_happy_path(
    consumer: AiCardKafkaConsumer,
    mock_pipeline_fn: AsyncMock,
) -> None:
    """유효한 이벤트 수신 시 파이프라인 호출 후 오프셋 커밋."""
    await consumer._handle_message(MockMsg(value=VALID_EVENT))

    mock_pipeline_fn.assert_awaited_once_with(
        note_id="note-1111",
        user_id="user-2222",
        tenant_id="tenant-3333",
        deck_id="deck-4444",
        content=None,
    )
    assert "evt-0001" in consumer._processed
    consumer._consumer.commit.assert_awaited_once()
    consumer._producer.send_and_wait.assert_not_awaited()


async def test_handle_message_duplicate_skipped(
    consumer: AiCardKafkaConsumer,
    mock_pipeline_fn: AsyncMock,
) -> None:
    """이미 처리한 event_id는 파이프라인 호출 없이 스킵."""
    consumer._processed["evt-0001"] = None

    await consumer._handle_message(MockMsg(value=VALID_EVENT))

    mock_pipeline_fn.assert_not_awaited()
    consumer._consumer.commit.assert_awaited_once()
    consumer._producer.send_and_wait.assert_not_awaited()


async def test_handle_message_invalid_schema_sends_to_dlq(
    consumer: AiCardKafkaConsumer,
    mock_pipeline_fn: AsyncMock,
) -> None:
    """스키마 검증 실패 시 DLQ 전송 후 오프셋 커밋."""
    invalid_event = {"bad_field": "no_good"}

    await consumer._handle_message(MockMsg(value=invalid_event))

    mock_pipeline_fn.assert_not_awaited()
    consumer._producer.send_and_wait.assert_awaited_once_with(
        settings.kafka_dlq_topic, value=invalid_event
    )
    consumer._consumer.commit.assert_awaited_once()


async def test_handle_message_pipeline_failure_sends_to_dlq(
    consumer: AiCardKafkaConsumer,
) -> None:
    """파이프라인이 재시도 후에도 실패하면 DLQ 전송 후 오프셋 커밋."""
    consumer._process_with_retry = AsyncMock(side_effect=RuntimeError("LLM unavailable"))  # type: ignore[method-assign]

    await consumer._handle_message(MockMsg(value=VALID_EVENT))

    assert "evt-0001" not in consumer._processed
    consumer._producer.send_and_wait.assert_awaited_once_with(
        settings.kafka_dlq_topic, value=VALID_EVENT
    )
    consumer._consumer.commit.assert_awaited_once()

"""AI 카드 자동 생성 E2E 테스트 — Step 8

시나리오:
  1. happy_path: note.created 이벤트 → pipeline_fn 호출 → 정상 처리 확인
  2. dlq_on_persistent_failure: pipeline_fn 영구 실패 → DLQ 토픽에 원본 이벤트 전달 확인
  3. performance: 이벤트 발행 → pipeline_fn 첫 호출까지 10초 이내
"""

import asyncio
import json
import time
import uuid
from unittest.mock import AsyncMock

import pytest
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer

from app.core.config import settings as _settings
from app.kafka.consumer import AiCardKafkaConsumer

TOPIC = "note.created.v1"
DLQ_TOPIC = "note.created.dlq"
PROCESS_TIMEOUT = 20.0

_BASE_EVENT = {
    "note_id": "e2e-note-111",
    "user_id": "e2e-user-222",
    "tenant_id": "e2e-tenant-333",
    "deck_id": "e2e-deck-444",
    "title": "E2E 테스트 노트",
}

# Schema Registry 없는 테스트 환경용 JSON deserializer
_JSON_DESER = lambda m: json.loads(m.decode())  # noqa: E731


def _unique_event() -> dict[str, str]:
    return {**_BASE_EVENT, "event_id": str(uuid.uuid4())}


async def _publish(bootstrap: str, topic: str, payload: dict[str, str]) -> None:
    producer = AIOKafkaProducer(
        bootstrap_servers=bootstrap,
        value_serializer=lambda v: json.dumps(v).encode(),
    )
    await producer.start()
    try:
        await producer.send_and_wait(topic, value=payload)
    finally:
        await producer.stop()


async def _consume_matching(
    bootstrap: str,
    topic: str,
    group: str,
    event_id: str,
    timeout: float = 15.0,
) -> dict[str, str]:
    """earliest 오프셋부터 event_id가 일치하는 메시지를 반환."""
    consumer = AIOKafkaConsumer(
        topic,
        bootstrap_servers=bootstrap,
        group_id=group,
        value_deserializer=lambda m: json.loads(m.decode()),
        auto_offset_reset="earliest",
        enable_auto_commit=False,
    )
    await consumer.start()
    try:
        async def _read() -> dict[str, str]:
            async for msg in consumer:
                if msg.value.get("event_id") == event_id:
                    return msg.value  # type: ignore[no-any-return]
            return {}

        return await asyncio.wait_for(_read(), timeout=timeout)
    finally:
        await consumer.stop()


def _patch_kafka(monkeypatch: pytest.MonkeyPatch, bootstrap: str, group_suffix: str) -> None:
    monkeypatch.setattr(_settings, "kafka_bootstrap_servers", bootstrap)
    monkeypatch.setattr(_settings, "kafka_note_created_topic", TOPIC)
    monkeypatch.setattr(_settings, "kafka_consumer_group_id", f"e2e-{group_suffix}")
    monkeypatch.setattr(_settings, "kafka_dlq_topic", DLQ_TOPIC)


# ─── 시나리오 1: 정상 플로우 ──────────────────────────────────────────────────


@pytest.mark.e2e
async def test_happy_path(kafka_bootstrap: str, monkeypatch: pytest.MonkeyPatch) -> None:
    """note.created 이벤트 수신 → pipeline_fn 올바른 인자로 호출 → processed 등록."""
    event = _unique_event()
    _patch_kafka(monkeypatch, kafka_bootstrap, "happy")
    processed = asyncio.Event()

    async def pipeline_fn(
        *, note_id: str, user_id: str, tenant_id: str, deck_id: str
    ) -> list[str]:
        assert note_id == event["note_id"]
        assert user_id == event["user_id"]
        assert deck_id == event["deck_id"]
        processed.set()
        return ["card-1", "card-2"]

    consumer = AiCardKafkaConsumer(pipeline_fn=pipeline_fn, value_deserializer=_JSON_DESER)
    await consumer.start()
    try:
        await _publish(kafka_bootstrap, TOPIC, event)
        await asyncio.wait_for(processed.wait(), timeout=PROCESS_TIMEOUT)
    finally:
        await consumer.stop()

    assert event["event_id"] in consumer._processed


# ─── 시나리오 2: DLQ 전달 ────────────────────────────────────────────────────


@pytest.mark.e2e
async def test_dlq_on_persistent_failure(
    kafka_bootstrap: str, monkeypatch: pytest.MonkeyPatch
) -> None:
    """pipeline_fn 영구 실패 시 원본 이벤트가 DLQ 토픽에 전달된다."""
    event = _unique_event()
    uid = event["event_id"]
    _patch_kafka(monkeypatch, kafka_bootstrap, uid[:8])

    async def always_fail(
        *, note_id: str, user_id: str, tenant_id: str, deck_id: str
    ) -> list[str]:
        raise RuntimeError("LLM unavailable")

    consumer = AiCardKafkaConsumer(pipeline_fn=always_fail, value_deserializer=_JSON_DESER)
    # tenacity 대기 없이 즉시 실패 — 재시도 로직은 단위 테스트에서 검증됨
    consumer._process_with_retry = AsyncMock(  # type: ignore[method-assign]
        side_effect=RuntimeError("LLM unavailable")
    )
    await consumer.start()

    await _publish(kafka_bootstrap, TOPIC, event)

    dlq_msg = await _consume_matching(
        kafka_bootstrap, DLQ_TOPIC, f"e2e-dlq-v-{uid[:8]}", event_id=uid
    )
    await consumer.stop()

    assert dlq_msg["event_id"] == uid


# ─── 시나리오 3: 성능 측정 ────────────────────────────────────────────────────


@pytest.mark.e2e
async def test_performance(kafka_bootstrap: str, monkeypatch: pytest.MonkeyPatch) -> None:
    """이벤트 발행 → pipeline_fn 첫 호출까지 10초 이내."""
    event = _unique_event()
    _patch_kafka(monkeypatch, kafka_bootstrap, "perf")
    pipeline_called_at: list[float] = []
    processed = asyncio.Event()

    async def pipeline_fn(
        *, note_id: str, user_id: str, tenant_id: str, deck_id: str
    ) -> list[str]:
        pipeline_called_at.append(time.monotonic())
        processed.set()
        return ["card-1"]

    consumer = AiCardKafkaConsumer(pipeline_fn=pipeline_fn, value_deserializer=_JSON_DESER)
    await consumer.start()
    try:
        publish_at = time.monotonic()
        await _publish(kafka_bootstrap, TOPIC, event)
        await asyncio.wait_for(processed.wait(), timeout=PROCESS_TIMEOUT)
    finally:
        await consumer.stop()

    elapsed = pipeline_called_at[0] - publish_at
    assert elapsed < 10.0, f"처리 지연 {elapsed:.2f}s > 10s SLA"

import json
from unittest.mock import AsyncMock, MagicMock

import httpx
import pytest
import respx
from httpx import Response

from app.clients.card_client import CardApiClient
from app.schemas.ai import CardGenerateResponse, GeneratedCard, UsageInfo
from app.services.ai_service import AIService
from app.services.card_pipeline_service import AiCardPipelineService

DECK_ID = "deck-aaaabbbb-1111"
USER_ID = "user-aaaabbbb-2222"
TENANT_ID = "tenant-aaaabbbb-3333"
NOTE_ID = "note-aaaabbbb-4444"
CARD_BASE_URL = "http://localhost:8082"

MOCK_CARDS = [
    GeneratedCard(
        front="파이썬 GIL이란?",
        back="한 번에 하나의 스레드만 Python 바이트코드를 실행하도록 제한하는 락",
    ),
    GeneratedCard(front="async/await의 목적은?", back="I/O 바운드 작업에서 블로킹 없이 동시성 처리"),
    GeneratedCard(front="list vs generator?", back="list는 즉시 평가, generator는 지연 평가·메모리 절약"),
]


def _card_response_json(card_id: str) -> dict:  # type: ignore[type-arg]
    return {
        "data": {
            "id": card_id,
            "deckId": DECK_ID,
            "cardType": "AI_GENERATED",
            "frontContent": "질문",
            "backContent": "답변",
            "bloomLevel": None,
            "status": "new",
            "easinessFactor": 2.5,
            "intervalDays": 0,
            "repetitions": 0,
            "lapses": 0,
            "dueDate": "2026-05-20T00:00:00Z",
            "lastReviewedAt": None,
            "createdAt": "2026-05-20T00:00:00Z",
            "updatedAt": "2026-05-20T00:00:00Z",
        }
    }


@pytest.fixture
def mock_ai_service() -> MagicMock:
    svc = MagicMock(spec=AIService)
    svc.generate_cards = AsyncMock(
        return_value=CardGenerateResponse(
            cards=MOCK_CARDS,
            model="claude-3-haiku-20240307",
            usage=UsageInfo(input_tokens=120, output_tokens=200),
        )
    )
    return svc


@pytest.fixture
def pipeline(mock_ai_service: MagicMock) -> AiCardPipelineService:
    return AiCardPipelineService(
        ai_service=mock_ai_service,
        card_client=CardApiClient(base_url=CARD_BASE_URL),
    )


@respx.mock
async def test_generate_and_save_happy_path(
    pipeline: AiCardPipelineService,
    mock_ai_service: MagicMock,
) -> None:
    """카드 생성 → learning-card 저장 happy path: 3장 생성 후 API 3회 호출."""
    card_route = respx.post(f"{CARD_BASE_URL}/decks/{DECK_ID}/cards").mock(
        side_effect=[
            Response(201, json=_card_response_json(f"card-id-{i}"))
            for i in range(len(MOCK_CARDS))
        ]
    )

    saved_ids = await pipeline.generate_and_save(
        note_content="파이썬 동시성 프로그래밍 핵심 개념 노트",
        deck_id=DECK_ID,
        user_id=USER_ID,
        tenant_id=TENANT_ID,
        note_id=NOTE_ID,
    )

    # LLM 카드 생성 호출 검증
    mock_ai_service.generate_cards.assert_called_once()

    # learning-card API 호출 횟수 검증 (카드 수만큼)
    assert card_route.call_count == len(MOCK_CARDS)

    # 첫 번째 요청 body 검증
    first_req = card_route.calls[0].request
    body = json.loads(first_req.content)
    assert body["frontContent"] == MOCK_CARDS[0].front
    assert body["backContent"] == MOCK_CARDS[0].back
    assert body["cardType"] == "AI_GENERATED"
    assert body["sourceId"] == NOTE_ID

    # 헤더 검증
    assert first_req.headers["X-User-Id"] == USER_ID
    assert first_req.headers["X-Tenant-Id"] == TENANT_ID

    # 반환값 검증
    assert len(saved_ids) == len(MOCK_CARDS)
    assert saved_ids[0] == "card-id-0"
    assert saved_ids[2] == "card-id-2"


@respx.mock
async def test_generate_and_save_api_failure(
    pipeline: AiCardPipelineService,
) -> None:
    """learning-card API 503 응답 시 HTTPStatusError 발생."""
    respx.post(f"{CARD_BASE_URL}/decks/{DECK_ID}/cards").mock(
        return_value=Response(503, json={"message": "Service Unavailable"})
    )

    with pytest.raises(httpx.HTTPStatusError):
        await pipeline.generate_and_save(
            note_content="노트 내용",
            deck_id=DECK_ID,
            user_id=USER_ID,
            tenant_id=TENANT_ID,
        )

import logging

import httpx

from app.schemas.ai import GeneratedCard

logger = logging.getLogger(__name__)


class CardApiClient:
    def __init__(self, base_url: str) -> None:
        self._base_url = base_url

    async def save_cards(
        self,
        *,
        deck_id: str,
        user_id: str,
        tenant_id: str,
        cards: list[GeneratedCard],
        source_note_id: str | None = None,
    ) -> list[str]:
        """카드 목록을 learning-card API에 저장. 생성된 card ID 목록 반환."""
        saved_ids: list[str] = []
        async with httpx.AsyncClient(base_url=self._base_url, timeout=30.0) as client:
            for card in cards:
                resp = await client.post(
                    f"/decks/{deck_id}/cards",
                    json={
                        "frontContent": card.front,
                        "backContent": card.back,
                        "cardType": "AI_GENERATED",
                        "sourceId": source_note_id,
                        "bloomLevel": None,
                    },
                    headers={"X-User-Id": user_id, "X-Tenant-Id": tenant_id},
                )
                resp.raise_for_status()
                saved_ids.append(resp.json()["data"]["id"])
                logger.info("Saved card %s to deck %s", saved_ids[-1], deck_id)
        return saved_ids

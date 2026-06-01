import logging
from typing import TYPE_CHECKING

from app.clients.card_client import CardApiClient
from app.schemas.ai import GenerateRequest
from app.services.ai_service import AIService

if TYPE_CHECKING:
    from app.kafka.notification_producer import NotificationProducer

logger = logging.getLogger(__name__)


class AiCardPipelineService:
    def __init__(
        self,
        ai_service: AIService,
        card_client: CardApiClient,
        notification: "NotificationProducer | None" = None,
    ) -> None:
        self._ai = ai_service
        self._card_client = card_client
        self._notification = notification

    async def generate_and_save(
        self,
        *,
        note_content: str,
        deck_id: str,
        user_id: str,
        tenant_id: str,
        note_id: str | None = None,
    ) -> list[str]:
        """노트 내용 → 카드 생성 → learning-card API 저장 → 알림 발행 파이프라인."""
        request = GenerateRequest(
            prompt=note_content, task="card_generation", max_tokens=1024, temperature=1.0
        )
        card_response = await self._ai.generate_cards(request)
        logger.info("Generated %d cards for note %s", len(card_response.cards), note_id)

        saved_ids = await self._card_client.save_cards(
            deck_id=deck_id,
            user_id=user_id,
            tenant_id=tenant_id,
            cards=card_response.cards,
            source_note_id=note_id,
        )

        if self._notification and saved_ids and note_id:
            try:
                await self._notification.send_ai_cards_ready(
                    note_id=note_id,
                    user_id=user_id,
                    tenant_id=tenant_id,
                    card_count=len(saved_ids),
                )
            except Exception:
                logger.exception("Failed to publish notification for note %s", note_id)

        return saved_ids

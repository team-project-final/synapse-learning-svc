import logging

import httpx

logger = logging.getLogger(__name__)


class NoteApiClient:
    def __init__(self, base_url: str) -> None:
        self._base_url = base_url

    async def get_note_content(self, *, note_id: str, user_id: str, tenant_id: str) -> str:
        async with httpx.AsyncClient(base_url=self._base_url, timeout=10.0) as client:
            resp = await client.get(
                f"/notes/{note_id}",
                headers={"X-User-Id": user_id, "X-Tenant-Id": tenant_id},
            )
            resp.raise_for_status()
            return str(resp.json()["data"]["content"])

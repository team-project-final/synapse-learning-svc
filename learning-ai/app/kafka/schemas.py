from pydantic import BaseModel, ConfigDict, Field


class NoteCreatedEvent(BaseModel):
    """knowledge.note.note-created-v1 Avro 스키마 매핑 (camelCase alias 지원)."""

    model_config = ConfigDict(populate_by_name=True)

    event_id: str = Field(alias="eventId", default="")
    note_id: str = Field(alias="noteId")
    user_id: str = Field(alias="userId")
    tenant_id: str = Field(alias="tenantId")
    deck_id: str | None = Field(alias="deckId", default=None)
    title: str = Field(alias="title", default="")
    content: str | None = Field(alias="content", default=None)
    occurred_at: int = Field(alias="occurredAt", default=0)

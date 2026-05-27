from pydantic import BaseModel


class NoteCreatedEvent(BaseModel):
    event_id: str
    note_id: str
    user_id: str
    tenant_id: str
    deck_id: str

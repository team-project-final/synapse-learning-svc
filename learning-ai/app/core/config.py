from pydantic import computed_field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings using pydantic-settings."""

    service_name: str = "synapse-learning-ai"
    version: str = "0.1.0"
    environment: str = "local"

    # API Keys
    anthropic_api_key: str | None = None
    openai_api_key: str | None = None
    anthropic_model: str = "claude-sonnet-4-6"

    @computed_field  # type: ignore[prop-decorator]
    @property
    def ai_enabled(self) -> bool:
        return bool(self.anthropic_api_key)

    @computed_field  # type: ignore[prop-decorator]
    @property
    def openai_enabled(self) -> bool:
        return bool(self.openai_api_key)

    # CORS
    backend_cors_origins: list[str] = ["http://localhost:3000", "http://localhost:5173"]

    # Database
    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/synapse"

    # Kafka
    kafka_enabled: bool = True
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_security_protocol: str = "PLAINTEXT"
    kafka_consumer_group_id: str = "learning-ai-svc-group"
    kafka_topic_prefix: str = ""  # env: LEARNING_AI_KAFKA_TOPIC_PREFIX (미설정 시 빈 문자열 폴백)
    kafka_note_created_topic: str = "knowledge.note.note-created-v1"
    kafka_note_deleted_topic: str = "knowledge.note.note-deleted-v1"
    kafka_dlq_topic: str = "note.created.dlq"
    kafka_note_deleted_dlq_topic: str = "note.deleted.dlq"
    kafka_notification_topic: str = "platform.notification.notification-send-v1"

    @model_validator(mode='after')
    def _apply_topic_prefix(self) -> 'Settings':
        if self.kafka_topic_prefix:
            self.kafka_note_created_topic = self.kafka_topic_prefix + self.kafka_note_created_topic
            self.kafka_note_deleted_topic = self.kafka_topic_prefix + self.kafka_note_deleted_topic
            self.kafka_notification_topic = self.kafka_topic_prefix + self.kafka_notification_topic
            self.kafka_dlq_topic = self.kafka_topic_prefix + self.kafka_dlq_topic
            self.kafka_note_deleted_dlq_topic = self.kafka_topic_prefix + self.kafka_note_deleted_dlq_topic
        return self

    # Schema Registry
    schema_registry_url: str = "http://localhost:8086"

    # Redis
    redis_url: str = "redis://localhost:6379/0"

    # 외부 서비스 URL
    note_service_url: str = "http://localhost:8081"
    learning_card_service_url: str = "http://localhost:8082"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="LEARNING_AI_",
        case_sensitive=False,
        extra="ignore",
    )


settings = Settings()

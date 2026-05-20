from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings using pydantic-settings."""

    service_name: str = "synapse-learning-ai"
    version: str = "0.1.0"
    environment: str = "local"

    # API Keys
    anthropic_api_key: str | None = None
    openai_api_key: str | None = None

    # CORS
    backend_cors_origins: list[str] = ["http://localhost:3000", "http://localhost:5173"]

    # Database
    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/synapse"

    # 외부 서비스 URL
    learning_card_service_url: str = "http://localhost:8082"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_prefix="LEARNING_AI_",
        case_sensitive=False,
        extra="ignore",
    )


settings = Settings()

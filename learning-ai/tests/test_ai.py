import uuid
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
import respx
from fastapi.testclient import TestClient
from httpx import Response

from app.api.deps import (
    get_ai_service,
    get_claude_service,
    get_embedding_service,
    get_note_chunk_repository,
)
from app.main import app
from app.models.note_chunk import NoteChunk
from app.services.ai_service import AIService
from app.services.claude_service import ClaudeService
from app.services.openai_service import OpenAIEmbeddingService

client = TestClient(app)


@pytest.fixture
def mock_repo() -> MagicMock:
    repo = MagicMock()
    repo.search_similar = AsyncMock()
    return repo


@pytest.fixture(autouse=True)
def override_deps(mock_repo: MagicMock) -> None:
    """Override dependencies to use dummy API keys and bypass tenacity waits."""
    claude = ClaudeService(api_key="dummy_anthropic")
    openai = OpenAIEmbeddingService(api_key="dummy_openai")
    ai_service = AIService(claude=claude, openai=openai, repo=mock_repo)

    app.dependency_overrides[get_claude_service] = lambda: claude
    app.dependency_overrides[get_embedding_service] = lambda: openai
    app.dependency_overrides[get_note_chunk_repository] = lambda: mock_repo
    app.dependency_overrides[get_ai_service] = lambda: ai_service

    # Speed up tests by removing tenacity wait
    with patch("tenacity.nap.time.sleep", return_value=None):
        yield

    app.dependency_overrides.clear()


@respx.mock
def test_generate_success() -> None:
    """Test successful AI generation with standardized response wrapper."""
    # Mock Anthropic API endpoint
    anthropic_route = respx.post("https://api.anthropic.com/v1/messages").mock(
        return_value=Response(
            200,
            json={
                "id": "msg_123",
                "type": "message",
                "role": "assistant",
                "content": [{"type": "text", "text": "Hello, I am Claude"}],
                "model": "claude-3-5-sonnet-20240620",
                "usage": {"input_tokens": 10, "output_tokens": 20},
            },
        )
    )

    response = client.post(
        "/ai/cards/generate",
        json={"prompt": "Hi", "max_tokens": 100, "temperature": 0.7},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["data"]["content"] == "Hello, I am Claude"
    assert anthropic_route.called


@respx.mock
def test_generate_rate_limit_retry() -> None:
    """Test retry logic on RateLimitError."""
    anthropic_route = respx.post("https://api.anthropic.com/v1/messages")
    anthropic_route.side_effect = [
        Response(429, json={"error": {"type": "rate_limit_error", "message": "Too many requests"}}),
        Response(
            200,
            json={
                "id": "msg_124",
                "type": "message",
                "role": "assistant",
                "content": [{"type": "text", "text": "Retry success"}],
                "model": "claude-3-5-sonnet-20240620",
                "usage": {"input_tokens": 5, "output_tokens": 5},
            },
        ),
    ]

    response = client.post("/ai/cards/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    assert response.json()["data"]["content"] == "Retry success"
    assert anthropic_route.call_count == 2


@respx.mock
def test_generate_fallback_to_openai() -> None:
    """Test fallback to OpenAI when Anthropic fails after retries."""
    # Mock Anthropic to fail 3 times (initial + 2 retries)
    anthropic_route = respx.post("https://api.anthropic.com/v1/messages").mock(
        return_value=Response(500, json={"error": {"type": "api_error"}})
    )

    # Mock OpenAI Chat Completion (fallback)
    openai_route = respx.post("https://api.openai.com/v1/chat/completions").mock(
        return_value=Response(
            200,
            json={
                "id": "chat_123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "gpt-4o-mini",
                "usage": {"prompt_tokens": 9, "completion_tokens": 12, "total_tokens": 21},
                "choices": [
                    {
                        "message": {"role": "assistant", "content": "OpenAI Fallback Response"},
                        "finish_reason": "stop",
                        "index": 0,
                    }
                ],
            },
        )
    )

    response = client.post("/ai/cards/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    data = response.json()
    assert data["data"]["content"] == "OpenAI Fallback Response"
    assert data["data"]["model"] == "gpt-4o-mini"
    assert anthropic_route.call_count == 3
    assert openai_route.called


@respx.mock
def test_semantic_search_success(mock_repo: MagicMock) -> None:
    """Test successful semantic search flow."""
    # Mock OpenAI Embeddings API
    openai_route = respx.post("https://api.openai.com/v1/embeddings").mock(
        return_value=Response(
            200,
            json={
                "object": "list",
                "data": [{"object": "embedding", "index": 0, "embedding": [0.1] * 1536}],
                "model": "text-embedding-3-small",
                "usage": {"prompt_tokens": 5, "total_tokens": 5},
            },
        )
    )

    # Mock Repository response
    chunk_id = uuid.uuid4()
    note_id = uuid.uuid4()
    mock_chunk = NoteChunk(
        id=chunk_id,
        note_id=note_id,
        content="Found content",
        tenant_id=uuid.uuid4(),
        chunk_index=0,
        embedding_model="test",
        chunk_strategy="test",
    )
    mock_repo.search_similar.return_value = [(mock_chunk, 0.95)]

    response = client.post(
        "/ai/search/semantic",
        json={"query": "test query", "top_k": 5, "threshold": 0.8},
        headers={"X-User-ID": str(uuid.uuid4())},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert len(data["data"]["results"]) == 1
    assert data["data"]["results"][0]["content"] == "Found content"
    assert data["data"]["results"][0]["score"] == 0.95
    assert openai_route.called
    assert mock_repo.search_similar.called

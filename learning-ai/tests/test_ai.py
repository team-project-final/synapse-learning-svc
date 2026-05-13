from unittest.mock import AsyncMock, patch

import pytest
from anthropic import InternalServerError, RateLimitError
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


@pytest.fixture
def mock_anthropic():
    """Fixture to mock Anthropic client."""
    with patch("app.services.claude_service.AsyncAnthropic") as mock:
        # Create an instance that will be returned when AsyncAnthropic() is called
        instance = mock.return_value
        # Ensure messages.create is an AsyncMock
        instance.messages.create = AsyncMock()
        yield instance


def test_generate_success(mock_anthropic):
    """Test successful AI generation."""
    # Setup mock response
    mock_msg = AsyncMock()
    mock_msg.content = [AsyncMock(text="Hello, I am Claude")]
    mock_msg.model = "claude-3-5-sonnet-20240620"
    mock_msg.usage = AsyncMock(input_tokens=10, output_tokens=20)
    mock_anthropic.messages.create.return_value = mock_msg

    response = client.post(
        "/ai/generate",
        json={"prompt": "Hi", "max_tokens": 100, "temperature": 0.7},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["content"] == "Hello, I am Claude"
    assert data["model"] == "claude-3-5-sonnet-20240620"
    assert data["usage"]["input_tokens"] == 10


def test_generate_rate_limit_retry(mock_anthropic):
    """Test 429 Rate Limit error retry logic."""
    mock_msg = AsyncMock()
    mock_msg.content = [AsyncMock(text="Retry success")]
    mock_msg.model = "claude-3-5-sonnet-20240620"
    mock_msg.usage = AsyncMock(input_tokens=5, output_tokens=5)

    # First call raises RateLimitError, second succeeds
    mock_anthropic.messages.create.side_effect = [
        RateLimitError(message="Rate limit", response=AsyncMock(), body={}),
        mock_msg,
    ]

    with patch("asyncio.sleep", AsyncMock()):
        response = client.post("/ai/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    assert response.json()["content"] == "Retry success"


def test_generate_internal_error_fallback(mock_anthropic):
    """Test 500 Internal Error fallback logic."""
    mock_anthropic.messages.create.side_effect = InternalServerError(
        message="Internal error", response=AsyncMock(), body={}
    )

    with patch("asyncio.sleep", AsyncMock()):
        response = client.post("/ai/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    assert "Fallback" in response.json()["content"]
    assert response.json()["model"] == "fallback"

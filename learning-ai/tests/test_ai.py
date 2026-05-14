import pytest
import respx
from httpx import Response
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch

from app.main import app
from app.api.deps import get_claude_service
from app.services.claude_service import ClaudeService

client = TestClient(app)


@pytest.fixture(autouse=True)
def override_claude_service():
    """Override get_claude_service to use a dummy API key for testing."""
    app.dependency_overrides[get_claude_service] = lambda: ClaudeService(api_key="dummy_key")
    yield
    app.dependency_overrides.pop(get_claude_service)


@respx.mock
def test_generate_success():
    """
    Test successful AI generation using respx to mock HTTP calls.
    Aligns with Phase 1 of the Mocking Strategy.
    """
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
                "stop_reason": "end_turn",
                "stop_sequence": None,
                "usage": {"input_tokens": 10, "output_tokens": 20},
            },
        )
    )

    response = client.post(
        "/ai/generate",
        json={"prompt": "Hi", "max_tokens": 100, "temperature": 0.7},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["content"] == "Hello, I am Claude"
    assert anthropic_route.called


@respx.mock
def test_generate_rate_limit_retry():
    """
    Test 429 Rate Limit error retry logic using respx.
    Verifies that the service retries on 429 and eventually succeeds.
    """
    # Mock Anthropic API endpoint with side effect: 429 then 200
    anthropic_route = respx.post("https://api.anthropic.com/v1/messages")
    anthropic_route.side_effect = [
        Response(
            429, json={"error": {"type": "rate_limit_error", "message": "Rate limit exceeded"}}
        ),
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

    # Patch asyncio.sleep to speed up tests
    with patch("asyncio.sleep", AsyncMock()):
        response = client.post("/ai/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    assert response.json()["content"] == "Retry success"
    assert anthropic_route.call_count == 2


@respx.mock
def test_generate_internal_server_error_fallback():
    """
    Test 500 Internal Server Error fallback logic.
    Verifies that the service retries and returns a fallback response after exhaustion.
    """
    # Mock Anthropic API endpoint with 500 error consistently
    anthropic_route = respx.post("https://api.anthropic.com/v1/messages").mock(
        return_value=Response(
            500, json={"error": {"type": "api_error", "message": "Internal server error"}}
        )
    )

    with patch("asyncio.sleep", AsyncMock()):
        response = client.post("/ai/generate", json={"prompt": "Hi"})

    assert response.status_code == 200
    data = response.json()
    assert "Fallback" in data["content"]
    assert data["model"] == "fallback"
    # Retries [1, 2, 4] + initial = 4 total calls
    assert anthropic_route.call_count == 4

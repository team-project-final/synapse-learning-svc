from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_check() -> None:
    """Check if the health API returns 'ok' and environment info."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "environment" in data
    assert data["environment"] == "development"  # Default value check


def test_health_ready_check() -> None:
    """Check if the readiness API returns 'ready'."""
    response = client.get("/health/ready")
    assert response.status_code == 200
    assert response.json() == {"status": "ready"}


def test_non_existent_endpoint() -> None:
    """Verify 404 error and custom exception handler for invalid paths."""
    response = client.get("/invalid-path")
    assert response.status_code == 404
    # Check if it follows our global exception handler format
    assert "detail" in response.json()

from typing import Annotated

from fastapi import Header


async def get_current_user(x_user_id: Annotated[str | None, Header()] = None) -> str:
    """
    Mock dependency for getting the current user from Gateway headers.

    In a real MSA environment, the API Gateway would handle authentication
    and pass the user's ID via a header like 'X-User-ID'.
    """
    if not x_user_id:
        # Default for local development
        return "mock_user_123"
    return x_user_id

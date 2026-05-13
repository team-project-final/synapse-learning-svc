from fastapi import HTTPException, Request
from fastapi.responses import JSONResponse


async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Capture and return all unhandled errors as 500 errors."""
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal Server Error", "message": str(exc)},
    )


async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    """Returns HTTPException in a consistent format."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail},
    )

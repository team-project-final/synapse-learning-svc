from fastapi import HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.schemas.base import ApiErrorDetail, ApiErrorResponse


async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Capture and return all unhandled errors as standard error JSON."""
    error_response = ApiErrorResponse(
        success=False,
        error=ApiErrorDetail(
            code="L_INTERNAL_ERROR",
            message="An unexpected error occurred.",
            details=[str(exc)],
        ),
    )
    return JSONResponse(
        status_code=500,
        content=error_response.model_dump(mode="json"),
    )


async def http_exception_handler(request: Request, exc: HTTPException) -> JSONResponse:
    """Returns HTTPException in a consistent standard format."""
    error_response = ApiErrorResponse(
        success=False,
        error=ApiErrorDetail(
            code="L_HTTP_ERROR",
            message=str(exc.detail),
        ),
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=error_response.model_dump(mode="json"),
    )


async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Returns RequestValidationError in a consistent standard format."""
    error_response = ApiErrorResponse(
        success=False,
        error=ApiErrorDetail(
            code="L_VALIDATION_FAILED",
            message="입력값 검증에 실패했습니다.",
            details=[str(err) for err in exc.errors()],
        ),
    )
    return JSONResponse(
        status_code=400,
        content=error_response.model_dump(mode="json"),
    )

# Stage 1: Build
FROM python:3.12-slim AS builder

WORKDIR /app

# Install build dependencies
COPY pyproject.toml .
COPY app app
RUN pip install --no-cache-dir --prefix=/install .

# Stage 2: Runtime
FROM python:3.12-slim

WORKDIR /app

# Copy installed dependencies from builder
COPY --from=builder /install /usr/local

# Copy application code
COPY . .

# Create non-root user (uid/gid 101) and own the app dir
RUN groupadd -g 101 -r app && useradd -u 101 -r -g app app && chown -R app:app /app
USER app

# Environment variables
ENV PYTHONPATH=/app
ENV PYTHONUNBUFFERED=1

# Expose the port specified in the workflow
EXPOSE 8090

# Run uvicorn on port 8090
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]

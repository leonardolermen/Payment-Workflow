FROM python:3.11-slim

WORKDIR /app

COPY analytics-service/requirements.txt .
RUN pip install -r requirements.txt
# Auto-detect and install instrumentation libraries (fastapi, httpx, etc.)
RUN opentelemetry-bootstrap -a install

COPY analytics-service/ .

EXPOSE 8085

# opentelemetry-instrument wraps uvicorn — zero code change needed
CMD ["opentelemetry-instrument", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8085"]

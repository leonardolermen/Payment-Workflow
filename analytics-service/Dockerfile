FROM python:3.11-slim

WORKDIR /app

# Copy SDKs first
COPY sdks/sdk-python ./sdks/sdk-python
COPY analytics-service ./analytics-service

WORKDIR /app/analytics-service
RUN pip install -r requirements.txt

EXPOSE 8085
CMD ["python", "main.py"]

import os
import asyncio
import logging
from fastapi import FastAPI, Request
from pydantic import BaseModel

from traceflow import Tracer, TracerHandler, patch_all
from traceflow.integrations.fastapi import TracerMiddleware

# Init Tracer — picks up TRACER_API_KEY and TRACER_COLLECTOR_URL from env
Tracer.init(service_name="analytics-service")

# Patch outbound HTTP (httpx used by FastAPI TestClient and internal calls)
patch_all()

# Wire logging handler so all Python log entries go to Tracer
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("analytics-service")
logger.addHandler(TracerHandler())

app = FastAPI()
app.add_middleware(TracerMiddleware)


class AnalyticsRequest(BaseModel):
    paymentId: str
    amount: float
    payerId: str
    payeeId: str


@app.get("/health")
async def health():
    return {"status": "ok", "service": "analytics-service"}


@app.post("/analytics/track")
async def track_payment(request: Request, req: AnalyticsRequest):
    trace_id = getattr(request.state, "traceId", None)

    logger.info(
        "Tracking payment",
        extra={"trace_id": trace_id, "paymentId": req.paymentId, "amount": str(req.amount)},
    )

    # Simulate ML model processing
    await asyncio.sleep(0.3)
    await asyncio.sleep(0.2)

    ml_score = 0.05
    logger.info(
        "ML scoring complete",
        extra={"trace_id": trace_id, "paymentId": req.paymentId, "ml_score": str(ml_score)},
    )

    return {"status": "tracked", "ml_score": ml_score}


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8085))
    uvicorn.run(app, host="0.0.0.0", port=port)

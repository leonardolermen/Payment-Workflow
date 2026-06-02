import os
import asyncio
from fastapi import FastAPI
from pydantic import BaseModel
from traceflow import TraceFlow
from traceflow.integrations.fastapi import TraceFlowMiddleware

app = FastAPI()

# Initialize TraceFlow SDK
tf = TraceFlow.init(
    service_name="analytics-service",
    collector_url=os.environ.get("TRACEFLOW_COLLECTOR_URL"),
    api_key=os.environ.get("TRACEFLOW_API_KEY")
)

# Add middleware for automatic tracing
app.add_middleware(TraceFlowMiddleware)

class AnalyticsRequest(BaseModel):
    paymentId: str
    amount: float
    payerId: str
    payeeId: str

@app.post("/analytics/track")
async def track_payment(req: AnalyticsRequest):
    print(f"[Analytics] Tracking payment {req.paymentId} for amount {req.amount}...")
    
    # Simulate some ML model processing
    await asyncio.sleep(0.3)
    
    # Start a custom child span
    with tf.start_span("run_ml_model") as span:
        span.set_tag("model_version", "v1.4.2")
        span.set_tag("fraud_score", 0.05)
        await asyncio.sleep(0.2)
        
    return {"status": "tracked", "ml_score": 0.05}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8085))
    uvicorn.run(app, host="0.0.0.0", port=port)

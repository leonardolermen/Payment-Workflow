import os
import asyncio
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class AnalyticsRequest(BaseModel):
    paymentId: str
    amount: float
    payerId: str
    payeeId: str

@app.post("/analytics/track")
async def track_payment(req: AnalyticsRequest):
    print(f"[Analytics] Tracking payment {req.paymentId} for amount {req.amount}...")

    # Simulate ML model processing
    await asyncio.sleep(0.3)
    await asyncio.sleep(0.2)

    return {"status": "tracked", "ml_score": 0.05}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8085))
    uvicorn.run(app, host="0.0.0.0", port=port)

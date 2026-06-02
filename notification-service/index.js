const express = require('express');
const { TraceFlow, traceflowMiddleware } = require('@traceflow/api');

TraceFlow.init({
  serviceName: 'notification-service',
  collectorUrl: process.env.TRACEFLOW_COLLECTOR_URL || 'http://localhost:4318',
  apiKey: process.env.TRACEFLOW_API_KEY
});

const app = express();
app.use(express.json());
app.use(traceflowMiddleware(TraceFlow.instance));

app.post('/notifications/send', (req, res) => {
  const { paymentId, payeeId } = req.body;
  console.log(`[NotificationService] Sending payment notification for payment ${paymentId} to user ${payeeId}`);

  // Simulate delay
  setTimeout(() => {
    res.status(200).json({ status: 'sent', message: 'Notification sent successfully' });
  }, 100);
});

const port = process.env.PORT || 8084;
app.listen(port, () => {
  console.log(`Notification Service running on port ${port}`);
});

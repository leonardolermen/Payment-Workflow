const express = require('express');
const winston = require('winston');
const { Tracer, TracerWinstonTransport } = require('./traceflow');

Tracer.init({ serviceName: 'notification-service' });

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json(),
  ),
  transports: [
    new winston.transports.Console({ format: winston.format.simple() }),
    new TracerWinstonTransport({ serviceName: 'notification-service' }),
  ],
});

const app = express();
app.use(express.json());
app.use(Tracer.middleware());

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'notification-service' });
});

app.post('/notifications/send', (req, res) => {
  const { paymentId, payeeId } = req.body;
  const traceId = req.traceId;

  logger.info('Processing notification', { trace_id: traceId, paymentId, payeeId });

  setTimeout(() => {
    logger.info('Notification sent', {
      trace_id: traceId,
      paymentId,
      payeeId,
      channel: 'email',
    });
    res.status(200).json({ status: 'sent', message: 'Notification sent successfully' });
  }, 100);
});

const port = process.env.PORT || 8084;
app.listen(port, () => {
  logger.info(`Notification Service running on port ${port}`);
});

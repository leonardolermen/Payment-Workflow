// tracer.js — OTel bootstrap, carregado via --require antes do index.js
// O código do index.js NÃO precisa ser alterado.
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-http');
const { OTLPLogExporter } = require('@opentelemetry/exporter-logs-otlp-http');
const { BatchLogRecordProcessor } = require('@opentelemetry/sdk-logs');
const { Resource } = require('@opentelemetry/resources');
const { SEMRESATTRS_SERVICE_NAME } = require('@opentelemetry/semantic-conventions');

const collectorUrl = process.env.TRACER_COLLECTOR_URL || 'http://localhost:4317';
const apiKey = process.env.TRACER_API_KEY || '';
const headers = apiKey ? { 'x-api-key': apiKey } : {};

const sdk = new NodeSDK({
  resource: new Resource({
    [SEMRESATTRS_SERVICE_NAME]: process.env.OTEL_SERVICE_NAME || 'notification-service',
  }),
  traceExporter: new OTLPTraceExporter({
    url: `${collectorUrl}/v1/traces`,
    headers,
  }),
  logRecordProcessor: new BatchLogRecordProcessor(
    new OTLPLogExporter({
      url: `${collectorUrl}/v1/logs`,
      headers,
    })
  ),
  instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();
console.log('[Tracer] OpenTelemetry auto-instrumentation started');

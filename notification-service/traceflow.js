'use strict';

const http  = require('http');
const https = require('https');
const crypto = require('crypto');

let _config = null;

function generateId(bytes) {
  return crypto.randomBytes(bytes).toString('hex');
}

function post(baseUrl, path, payload, apiKey) {
  try {
    const url  = new URL(baseUrl + path);
    const body = JSON.stringify(payload);
    const mod  = url.protocol === 'https:' ? https : http;
    const req  = mod.request({
      hostname: url.hostname,
      port:     url.port || (url.protocol === 'https:' ? 443 : 80),
      path:     url.pathname,
      method:   'POST',
      headers: {
        'Content-Type':   'application/json',
        'Content-Length': Buffer.byteLength(body),
        ...(apiKey ? { 'x-api-key': apiKey } : {}),
      },
    });
    req.on('error', () => {});
    req.write(body);
    req.end();
  } catch (_) {}
}

const SENSITIVE = new Set([
  'password', 'senha', 'token', 'secret', 'authorization',
  'cvv', 'card_number', 'cpf', 'ssn', 'api_key',
]);

function sanitize(obj, depth = 3) {
  if (!obj || typeof obj !== 'object' || depth === 0) return {};
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    if (SENSITIVE.has(k.toLowerCase())) {
      out[k] = '[REDACTED]';
    } else if (v !== null && typeof v === 'object') {
      const nested = sanitize(v, depth - 1);
      for (const [nk, nv] of Object.entries(nested)) out[`${k}.${nk}`] = nv;
    } else {
      out[k] = String(v ?? '');
    }
  }
  return out;
}

function extractContext(headers) {
  const traceId = headers['x-tracer-trace-id'];
  const spanId  = headers['x-tracer-span-id'];
  if (traceId) return { traceId, spanId };

  const tp = headers['traceparent'];
  if (tp) {
    const parts = tp.split('-');
    if (parts.length === 4) return { traceId: parts[1], spanId: parts[2] };
  }
  return null;
}

// ── SDK object ────────────────────────────────────────────────────────────────

const Tracer = {
  init(config = {}) {
    _config = {
      serviceName:  config.serviceName  || process.env.TRACER_SERVICE_NAME || 'unknown',
      collectorUrl: config.collectorUrl || process.env.TRACER_COLLECTOR_URL || 'http://localhost:4317',
      apiKey:       config.apiKey       || process.env.TRACER_API_KEY,
    };
    return this;
  },

  middleware() {
    return (req, res, next) => {
      if (!_config) return next();

      const incoming  = extractContext(req.headers);
      const traceId   = incoming?.traceId || generateId(16);
      const parentId  = incoming?.spanId;
      const spanId    = generateId(8);
      const startedAt = new Date();

      const span = {
        id:             spanId,
        trace_id:       traceId,
        parent_id:      parentId,
        service_name:   _config.serviceName,
        operation_name: `${req.method} ${req.path}`,
        kind:           'server',
        started_at:     startedAt.toISOString(),
        status:         'in_progress',
        tags:           { 'http.method': req.method, 'http.url': req.originalUrl },
        logs:           [],
      };

      res.setHeader('x-tracer-trace-id', traceId);
      req.span    = span;
      req.traceId = traceId;

      const reqAttrs = { method: req.method, url: req.originalUrl };
      if (req.body && typeof req.body === 'object') {
        const s = sanitize(req.body);
        for (const [k, v] of Object.entries(s)) reqAttrs[`body.${k}`] = v;
      }
      span.logs.push({ level: 'INFO', message: 'http.request', attributes: reqAttrs, timestamp: new Date().toISOString() });

      const originalJson = res.json.bind(res);
      res.json = function (body) {
        const status = res.statusCode;
        span.tags['http.status_code'] = String(status);
        const resAttrs = { status_code: String(status) };
        if (body && typeof body === 'object') {
          const s = sanitize(body);
          for (const [k, v] of Object.entries(s)) resAttrs[`body.${k}`] = v;
        }
        span.logs.push({ level: status >= 500 ? 'ERROR' : 'INFO', message: 'http.response', attributes: resAttrs, timestamp: new Date().toISOString() });
        span.ended_at    = new Date().toISOString();
        span.duration_ms = Date.now() - startedAt.getTime();
        span.status      = status >= 500 ? 'error' : 'ok';
        post(_config.collectorUrl, '/spans', span, _config.apiKey);
        return originalJson(body);
      };

      res.on('finish', () => {
        if (span.status === 'in_progress') {
          span.ended_at    = new Date().toISOString();
          span.duration_ms = Date.now() - startedAt.getTime();
          span.status      = res.statusCode >= 500 ? 'error' : 'ok';
          span.tags['http.status_code'] = String(res.statusCode);
          post(_config.collectorUrl, '/spans', span, _config.apiKey);
        }
      });

      next();
    };
  },
};

// ── Winston-compatible transport ──────────────────────────────────────────────

const EventEmitter = require('events');

const LEVEL_MAP = {
  error: 'ERROR', warn: 'WARN', info: 'INFO',
  http: 'INFO', verbose: 'DEBUG', debug: 'DEBUG', silly: 'DEBUG',
};

class TracerWinstonTransport extends EventEmitter {
  constructor(opts = {}) {
    super();
    this.name = 'tracer';
    this._serviceName  = opts.serviceName  || (_config?.serviceName)  || 'unknown';
    this._collectorUrl = opts.collectorUrl || (_config?.collectorUrl) || process.env.TRACER_COLLECTOR_URL || 'http://localhost:4317';
    this._apiKey       = opts.apiKey       || (_config?.apiKey)       || process.env.TRACER_API_KEY;
  }

  log(info, callback) {
    setImmediate(() => this.emit('logged', info));

    const { level, message, trace_id, traceId, timestamp, ...meta } = info;
    const attributes = {};
    for (const [k, v] of Object.entries(meta)) {
      if (v !== null && v !== undefined && typeof v !== 'object') attributes[k] = String(v);
    }

    const payload = {
      service_name: this._serviceName,
      level:        LEVEL_MAP[level] || 'INFO',
      message:      String(message || ''),
      attributes,
      timestamp:    (timestamp) || new Date().toISOString(),
    };

    const resolvedTraceId = trace_id || traceId;
    if (resolvedTraceId) payload.trace_id = String(resolvedTraceId);

    post(this._collectorUrl, '/v1/logs', payload, this._apiKey);
    callback();
  }
}

module.exports = { Tracer, TracerWinstonTransport };

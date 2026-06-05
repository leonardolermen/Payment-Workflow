import logging
import datetime
from ..tracer import _current_span

_LEVEL_MAP = {
    logging.DEBUG:    'DEBUG',
    logging.INFO:     'INFO',
    logging.WARNING:  'WARN',
    logging.ERROR:    'ERROR',
    logging.CRITICAL: 'ERROR',
}

_SKIP_FIELDS = frozenset({
    'msg', 'args', 'levelname', 'levelno', 'pathname', 'filename',
    'module', 'exc_info', 'exc_text', 'stack_info', 'lineno',
    'funcName', 'created', 'msecs', 'relativeCreated', 'thread',
    'threadName', 'processName', 'process', 'name', 'message',
    'trace_id', 'taskName',
})


class TracerHandler(logging.Handler):
    def __init__(self, tracer=None, level=logging.NOTSET):
        super().__init__(level)
        self._tracer = tracer

    def _get_tracer(self):
        if self._tracer is not None:
            return self._tracer
        from .. import Tracer
        return Tracer.instance

    def emit(self, record: logging.LogRecord) -> None:
        tracer = self._get_tracer()
        if tracer is None:
            return
        try:
            self._send(tracer, record)
        except Exception:
            self.handleError(record)

    def _send(self, tracer, record: logging.LogRecord) -> None:
        level    = _LEVEL_MAP.get(record.levelno, 'INFO')
        trace_id = getattr(record, 'trace_id', None)
        if trace_id is None:
            span = _current_span.get()
            if span is not None:
                trace_id = span.trace_id

        try:
            message = self.format(record)
        except Exception:
            message = record.getMessage()

        attributes: dict = {}
        for key, value in record.__dict__.items():
            if key in _SKIP_FIELDS or key.startswith('_'):
                continue
            try:
                attributes[key] = str(value)
            except Exception:
                pass

        if record.exc_info and record.exc_info[1] is not None:
            exc = record.exc_info[1]
            attributes['exception.type']    = type(exc).__name__
            attributes['exception.message'] = str(exc)

        payload = {
            'service_name': tracer.service_name,
            'level':        level,
            'message':      message,
            'attributes':   attributes,
            'timestamp':    datetime.datetime.utcnow().isoformat() + 'Z',
        }
        if trace_id:
            payload['trace_id'] = str(trace_id)

        tracer.transport.send_log(payload)

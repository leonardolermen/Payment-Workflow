from __future__ import annotations
from ..tracer import _current_span
from ..propagation import inject_headers

_patched_requests = False
_patched_httpx    = False


def _get_tracer():
    from .. import Tracer
    return Tracer.instance


def _start_client_span(tracer, method: str, url: str):
    if tracer is None or _current_span.get() is None:
        return None
    return tracer.start_span(
        f"{method.upper()} {url}",
        kind="client",
        tags={"http.method": method.upper(), "http.url": str(url)},
    )


def _finish_span(span, status_code, error=None):
    if span is None:
        return
    if error is not None:
        span.set_error(error)
        span.end("error")
        return
    if status_code is not None:
        span.set_tag("http.status_code", str(status_code))
    span.end("error" if (status_code or 0) >= 500 else "ok")


def patch_requests() -> None:
    global _patched_requests
    if _patched_requests:
        return
    try:
        import requests
    except ImportError:
        return

    _original_send = requests.Session.send

    def _traced_send(self, request, **kwargs):
        tracer = _get_tracer()
        span   = _start_client_span(tracer, request.method, request.url)
        if span is not None:
            headers = dict(request.headers)
            inject_headers(span, headers)
            request.headers.update(headers)
        try:
            response = _original_send(self, request, **kwargs)
            _finish_span(span, response.status_code)
            return response
        except Exception as exc:
            _finish_span(span, None, exc)
            raise

    requests.Session.send = _traced_send
    _patched_requests = True


def patch_httpx() -> None:
    global _patched_httpx
    if _patched_httpx:
        return
    try:
        import httpx
    except ImportError:
        return

    _orig_sync  = httpx.Client.send
    _orig_async = httpx.AsyncClient.send

    def _sync(self, request, **kwargs):
        tracer = _get_tracer()
        span   = _start_client_span(tracer, request.method, str(request.url))
        if span is not None:
            inject_headers(span, request.headers)
        try:
            response = _orig_sync(self, request, **kwargs)
            _finish_span(span, response.status_code)
            return response
        except Exception as exc:
            _finish_span(span, None, exc)
            raise

    async def _async(self, request, **kwargs):
        tracer = _get_tracer()
        span   = _start_client_span(tracer, request.method, str(request.url))
        if span is not None:
            inject_headers(span, request.headers)
        try:
            response = await _orig_async(self, request, **kwargs)
            _finish_span(span, response.status_code)
            return response
        except Exception as exc:
            _finish_span(span, None, exc)
            raise

    httpx.Client.send       = _sync
    httpx.AsyncClient.send  = _async
    _patched_httpx = True


def patch_all() -> None:
    patch_requests()
    patch_httpx()

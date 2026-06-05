from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from .. import Tracer
from ..propagation import extract_context
from ..sanitize import sanitize

class TracerMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        tracer = Tracer.instance
        if not tracer:
            return await call_next(request)

        headers  = {k.lower(): v for k, v in request.headers.items()}
        incoming = extract_context(headers)

        trace_id = incoming.get("traceId") if incoming else None
        parent_id = incoming.get("spanId") if incoming else None

        span = tracer.start_span(
            operation_name=f"{request.method} {request.url.path}",
            kind="server",
            trace_id=trace_id,
            parent_id=parent_id,
            tags={
                "http.method": request.method,
                "http.url":    str(request.url),
                "http.path":   request.url.path,
            }
        )

        request.state.span    = span
        request.state.traceId = span.trace_id

        req_attrs = {
            "method":       request.method,
            "url":          str(request.url),
            "user-agent":   request.headers.get("user-agent", ""),
            "content-type": request.headers.get("content-type", ""),
        }
        if request.query_params:
            for k, v in request.query_params.items():
                req_attrs[f"param.{k}"] = v

        span.log("http.request", req_attrs, "INFO")

        from ..tracer import _current_span
        token = _current_span.set(span)
        try:
            response: Response = await call_next(request)

            status = response.status_code
            span.set_tag("http.status_code", str(status))
            response.headers["x-tracer-trace-id"] = span.trace_id

            res_attrs = {"status_code": str(status)}
            if status >= 500:
                span.set_error(Exception(f"HTTP {status}"))
                span.log("http.response", res_attrs, "ERROR")
                span.end("error")
            else:
                span.log("http.response", res_attrs, "INFO")
                span.end("ok")

            return response

        except Exception as e:
            span.set_error(e)
            span.log("http.response", {"error": str(e)}, "ERROR")
            span.end("error")
            raise
        finally:
            _current_span.reset(token)

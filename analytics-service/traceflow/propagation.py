def extract_context(headers: dict):
    traceparent = headers.get("traceparent")
    if traceparent:
        parts = traceparent.split("-")
        if len(parts) == 4:
            return {"traceId": parts[1], "spanId": parts[2]}

    trace_id = headers.get("x-tracer-trace-id") or headers.get("x-b3-traceid")
    span_id  = headers.get("x-tracer-span-id")  or headers.get("x-b3-spanid")
    if trace_id:
        return {"traceId": trace_id, "spanId": span_id}

    return None

def inject_headers(span, headers: dict):
    headers["x-tracer-trace-id"] = span.trace_id
    headers["x-tracer-span-id"]  = span.id
    headers["traceparent"] = f"00-{span.trace_id}-{span.id}-01"

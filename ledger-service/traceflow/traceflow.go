package traceflow

import (
	"context"
	"net/http"

	"github.com/traceflow/sdk-go/middleware"
	"github.com/traceflow/sdk-go/tracer"
)

func Middleware(next http.Handler) http.Handler {
	return middleware.Middleware(next)
}

func SpanFromContext(ctx context.Context) *tracer.SpanEvent {
	return tracer.SpanFromContext(ctx)
}

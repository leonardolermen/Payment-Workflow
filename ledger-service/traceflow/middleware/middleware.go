package middleware

import (
	"bytes"
	"io"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/traceflow/sdk-go/tracer"
)

var sensitiveRegex = regexp.MustCompile(`(?i)"(password|token|cvv)"\s*:\s*(?:"[^"]*"|[^,}]+)`)

func redactAndTruncate(body []byte) string {
	if len(body) == 0 {
		return ""
	}
	if len(body) > 2048 {
		body = body[:2048]
	}
	return string(sensitiveRegex.ReplaceAll(body, []byte(`"$1":"[REDACTED]"`)))
}

type responseWriter struct {
	http.ResponseWriter
	status int
	body   *bytes.Buffer
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.status = code
	rw.ResponseWriter.WriteHeader(code)
}

func (rw *responseWriter) Write(b []byte) (int, error) {
	if rw.body.Len() < 2048 {
		toWrite := 2048 - rw.body.Len()
		if len(b) < toWrite {
			toWrite = len(b)
		}
		rw.body.Write(b[:toWrite])
	}
	return rw.ResponseWriter.Write(b)
}

func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		traceID := r.Header.Get("x-tracer-trace-id")
		spanID  := r.Header.Get("x-tracer-span-id")

		if traceID == "" {
			if tp := r.Header.Get("traceparent"); tp != "" {
				parts := strings.Split(tp, "-")
				if len(parts) == 4 {
					traceID = parts[1]
					spanID  = parts[2]
				}
			}
		}
		if traceID == "" {
			traceID = tracer.GenerateID(16)
		}

		span := &tracer.SpanEvent{
			TraceID:   traceID,
			SpanID:    tracer.GenerateID(8),
			ParentID:  spanID,
			Name:      r.Method + " " + r.URL.Path,
			Timestamp: time.Now(),
			Tags: map[string]string{
				"http.method": r.Method,
				"http.url":    r.URL.String(),
			},
			Logs: []tracer.LogEntry{},
		}

		var reqBody []byte
		if r.Body != nil {
			reqBody, _ = io.ReadAll(io.LimitReader(r.Body, 2048))
			r.Body = struct {
				io.Reader
				io.Closer
			}{
				Reader: io.MultiReader(bytes.NewReader(reqBody), r.Body),
				Closer: r.Body,
			}
		}

		span.Logs = append(span.Logs, tracer.LogEntry{
			Timestamp:  time.Now(),
			Level:      "INFO",
			Message:    "http.request",
			Attributes: map[string]string{"body": redactAndTruncate(reqBody)},
		})

		rw := &responseWriter{
			ResponseWriter: w,
			status:         http.StatusOK,
			body:           &bytes.Buffer{},
		}

		w.Header().Set("x-tracer-trace-id", span.TraceID)

		ctx := tracer.ContextWithSpan(r.Context(), span)
		next.ServeHTTP(rw, r.WithContext(ctx))

		span.Logs = append(span.Logs, tracer.LogEntry{
			Timestamp:  time.Now(),
			Level:      "INFO",
			Message:    "http.response",
			Attributes: map[string]string{"body": redactAndTruncate(rw.body.Bytes())},
		})

		span.Tags["http.status_code"] = strconv.Itoa(rw.status)
		tracer.SendSpan(span)
	})
}

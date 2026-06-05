package main

import (
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/traceflow/sdk-go/tracer"
)

func traceFlowMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		traceID := c.GetHeader("x-tracer-trace-id")
		spanID := c.GetHeader("x-tracer-span-id")

		if traceID == "" {
			if tp := c.GetHeader("traceparent"); tp != "" {
				parts := strings.Split(tp, "-")
				if len(parts) == 4 {
					traceID = parts[1]
					spanID = parts[2]
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
			Name:      c.Request.Method + " " + c.FullPath(),
			Timestamp: time.Now(),
			Tags: map[string]string{
				"http.method": c.Request.Method,
				"http.url":    c.Request.URL.String(),
				"http.path":   c.FullPath(),
			},
			Logs: []tracer.LogEntry{},
		}

		c.Header("x-tracer-trace-id", traceID)

		// Store span in context so outbound calls (if any) create child spans
		ctx := tracer.ContextWithSpan(c.Request.Context(), span)
		c.Request = c.Request.WithContext(ctx)

		// Set trace ID on Gin context for easy access in handlers
		c.Set("traceId", traceID)

		start := time.Now()
		c.Next()

		status := c.Writer.Status()
		durationMs := time.Since(start).Milliseconds()

		span.Tags["http.status_code"] = strconv.Itoa(status)
		span.Tags["http.duration_ms"] = strconv.FormatInt(durationMs, 10)

		level := "INFO"
		if status >= 500 {
			level = "ERROR"
		} else if status >= 400 {
			level = "WARN"
		}

		span.Logs = append(span.Logs, tracer.LogEntry{
			Timestamp: time.Now(),
			Level:     level,
			Message:   "http.response",
			Attributes: map[string]string{
				"status_code": strconv.Itoa(status),
				"duration_ms": strconv.FormatInt(durationMs, 10),
			},
		})

		tracer.SendSpan(span)
	}
}

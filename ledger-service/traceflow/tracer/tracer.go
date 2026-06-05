package tracer

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"os"
	"time"
)

type contextKey struct{}

func SpanFromContext(ctx context.Context) *SpanEvent {
	v, _ := ctx.Value(contextKey{}).(*SpanEvent)
	return v
}

func ContextWithSpan(ctx context.Context, span *SpanEvent) context.Context {
	return context.WithValue(ctx, contextKey{}, span)
}

type SpanEvent struct {
	TraceID   string            `json:"trace_id"`
	SpanID    string            `json:"span_id"`
	ParentID  string            `json:"parent_id,omitempty"`
	Name      string            `json:"name"`
	Timestamp time.Time         `json:"timestamp"`
	Logs      []LogEntry        `json:"logs"`
	Tags      map[string]string `json:"tags"`
}

type LogEntry struct {
	Timestamp  time.Time         `json:"timestamp"`
	Level      string            `json:"level"`
	Message    string            `json:"message"`
	Attributes map[string]string `json:"attributes,omitempty"`
}

var collectorURL = os.Getenv("TRACER_COLLECTOR_URL")
var apiKey       = os.Getenv("TRACER_API_KEY")
var httpClient   = &http.Client{Timeout: 5 * time.Second}

func SendSpan(span *SpanEvent) {
	if collectorURL == "" {
		return
	}
	go func(s SpanEvent) {
		payload, err := json.Marshal(s)
		if err != nil {
			return
		}
		req, err := http.NewRequest("POST", collectorURL+"/spans", bytes.NewBuffer(payload))
		if err != nil {
			return
		}
		req.Header.Set("Content-Type", "application/json")
		if apiKey != "" {
			req.Header.Set("x-api-key", apiKey)
		}
		resp, err := httpClient.Do(req)
		if err == nil {
			resp.Body.Close()
		}
	}(*span)
}

func GenerateID(bytesLen int) string {
	b := make([]byte, bytesLen)
	if _, err := rand.Read(b); err != nil {
		return "fallback-id"
	}
	return hex.EncodeToString(b)
}

package main

import (
	"bytes"
	"compress/gzip"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/google/uuid"
)

// Span represents a Tracer span with payload capture
type Span struct {
	ID            string            `json:"id"`
	TraceID       string            `json:"trace_id"`
	ParentID      string            `json:"parent_id,omitempty"`
	ServiceName   string            `json:"service_name"`
	OperationName string            `json:"operation_name"`
	Kind          string            `json:"kind"`
	StartedAt     string            `json:"started_at"`
	EndedAt       string            `json:"ended_at"`
	DurationMs    int64             `json:"duration_ms"`
	Status        string            `json:"status"`
	Tags          map[string]string `json:"tags"`
	Payloads      *PayloadInfo      `json:"payloads,omitempty"`
	WorkspaceID   string            `json:"workspace_id"`
}

// PayloadInfo holds request/response payloads
type PayloadInfo struct {
	RequestBody    string            `json:"request_body,omitempty"`
	RequestHeaders map[string]string `json:"request_headers,omitempty"`
	ResponseBody   string            `json:"response_body,omitempty"`
	ResponseStatus int               `json:"response_status"`
}

// Config holds sidecar configuration
type Config struct {
	Port          string
	TargetURL     string
	CollectorURL  string
	ServiceName   string
	WorkspaceID   string
	APIKey        string
	CaptureBody   bool
	MaxBodySize   int64
	SensitiveKeys []string
}

var config Config

func main() {
	loadConfig()

	target, err := url.Parse(config.TargetURL)
	if err != nil {
		log.Fatalf("Invalid target URL: %v", err)
	}

	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.ModifyResponse = modifyResponse
	proxy.ErrorHandler = errorHandler

	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		handleRequestWithTracing(w, r, proxy)
	})

	log.Printf("Tracer Sidecar starting on port %s -> %s", config.Port, config.TargetURL)
	log.Fatal(http.ListenAndServe(":"+config.Port, nil))
}

func loadConfig() {
	config.Port = getEnv("SIDECAR_PORT", "8080")
	config.TargetURL = getEnv("SIDECAR_TARGET_URL", "http://localhost:8081")
	config.CollectorURL = getEnv("TRACER_COLLECTOR_URL", "http://localhost:4317")
	config.ServiceName = getEnv("OTEL_SERVICE_NAME", "sidecar-service")
	config.WorkspaceID = getEnv("TRACER_WORKSPACE_ID", "ws_dev")
	config.APIKey = getEnv("TRACER_API_KEY", "")
	config.CaptureBody = getEnvBool("SIDECAR_CAPTURE_BODY", true)
	config.MaxBodySize = getEnvInt64("SIDECAR_MAX_BODY_SIZE", 1024*1024) // 1MB default

	sensitive := getEnv("SIDECAR_SENSITIVE_KEYS", "password,token,secret,authorization,cvv,card_number,cpf,ssn")
	config.SensitiveKeys = strings.Split(sensitive, ",")
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvBool(key string, defaultValue bool) bool {
	if value := os.Getenv(key); value != "" {
		return value == "true" || value == "1"
	}
	return defaultValue
}

func getEnvInt64(key string, defaultValue int64) int64 {
	if value := os.Getenv(key); value != "" {
		var result int64
		fmt.Sscanf(value, "%d", &result)
		return result
	}
	return defaultValue
}

func handleRequestWithTracing(w http.ResponseWriter, r *http.Request, proxy *httputil.ReverseProxy) {
	startTime := time.Now()

	// Extract or generate trace context
	traceID := r.Header.Get("X-Traceflow-Trace-Id")
	if traceID == "" {
		traceID = r.Header.Get("X-B3-Traceid")
	}
	if traceID == "" {
		traceID = generateTraceID()
	}

	parentID := r.Header.Get("X-Traceflow-Span-Id")
	if parentID == "" {
		parentID = r.Header.Get("X-B3-Spanid")
	}

	spanID := generateSpanID()

	// Inject tracing headers to downstream request
	r.Header.Set("X-Traceflow-Trace-Id", traceID)
	r.Header.Set("X-Traceflow-Span-Id", spanID)
	r.Header.Set("X-B3-Traceid", traceID)
	r.Header.Set("X-B3-Spanid", spanID)
	if parentID != "" {
		r.Header.Set("X-B3-Parentspanid", parentID)
	}

	// Capture request payload
	var requestBody string
	if config.CaptureBody && r.Body != nil {
		bodyBytes, _ := io.ReadAll(io.LimitReader(r.Body, config.MaxBodySize))
		r.Body = io.NopCloser(bytes.NewBuffer(bodyBytes))
		requestBody = sanitizePayload(string(bodyBytes))
	}

	// Create response wrapper to capture response
	recorder := &responseRecorder{
		ResponseWriter: w,
		statusCode:     http.StatusOK,
		body:           &bytes.Buffer{},
	}

	// Serve the request
	proxy.ServeHTTP(recorder, r)

	// Build and send span
	duration := time.Since(startTime)
	operationName := fmt.Sprintf("%s %s", r.Method, r.URL.Path)

	// Capture response headers
	responseHeaders := make(map[string]string)
	for k, v := range w.Header() {
		if len(v) > 0 && !isSensitiveHeader(k) {
			responseHeaders[k] = v[0]
		}
	}

	span := Span{
		ID:            spanID,
		TraceID:       traceID,
		ParentID:      parentID,
		ServiceName:   config.ServiceName,
		OperationName: operationName,
		Kind:          "server",
		StartedAt:     startTime.Format(time.RFC3339Nano),
		EndedAt:       time.Now().Format(time.RFC3339Nano),
		DurationMs:    duration.Milliseconds(),
		Status:        getStatusFromCode(recorder.statusCode),
		WorkspaceID:   config.WorkspaceID,
		Tags: map[string]string{
			"http.method":       r.Method,
			"http.url":          r.URL.String(),
			"http.path":         r.URL.Path,
			"http.status_code":  fmt.Sprintf("%d", recorder.statusCode),
			"http.user_agent":   r.UserAgent(),
			"http.content_type": r.Header.Get("Content-Type"),
			"peer.address":      r.RemoteAddr,
		},
	}

	if config.CaptureBody {
		responseBody := recorder.body.String()
		if len(responseBody) > int(config.MaxBodySize) {
			responseBody = responseBody[:config.MaxBodySize] + "...[truncated]"
		}

		span.Payloads = &PayloadInfo{
			RequestBody:    requestBody,
			RequestHeaders: sanitizeHeaders(r.Header),
			ResponseBody:   sanitizePayload(responseBody),
			ResponseStatus: recorder.statusCode,
		}
	}

	// Send span asynchronously
	go sendSpan(span)
}

func modifyResponse(resp *http.Response) error {
	return nil
}

func errorHandler(w http.ResponseWriter, r *http.Request, err error) {
	log.Printf("Proxy error: %v", err)
	http.Error(w, "Service unavailable", http.StatusServiceUnavailable)
}

func sendSpan(span Span) {
	payload, err := json.Marshal(span)
	if err != nil {
		log.Printf("Failed to marshal span: %v", err)
		return
	}

	url := config.CollectorURL + "/spans"
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(payload))
	if err != nil {
		log.Printf("Failed to create request: %v", err)
		return
	}

	req.Header.Set("Content-Type", "application/json")
	if config.APIKey != "" {
		req.Header.Set("X-API-Key", config.APIKey)
	}

	client := &http.Client{Timeout: 5 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Failed to send span: %v", err)
		return
	}
	defer resp.Body.Close()
}

func generateTraceID() string {
	return uuid.New().String()
}

func generateSpanID() string {
	return uuid.New().String()
}

func getStatusFromCode(code int) string {
	if code >= 500 {
		return "error"
	}
	if code >= 400 {
		return "error"
	}
	return "ok"
}

func isSensitiveHeader(key string) bool {
	sensitive := []string{"authorization", "cookie", "x-api-key", "api-key"}
	lowerKey := strings.ToLower(key)
	for _, s := range sensitive {
		if strings.Contains(lowerKey, s) {
			return true
		}
	}
	return false
}

func sanitizeHeaders(headers http.Header) map[string]string {
	result := make(map[string]string)
	for k, v := range headers {
		if len(v) > 0 && !isSensitiveHeader(k) {
			result[k] = v[0]
		}
	}
	return result
}

func sanitizePayload(payload string) string {
	if payload == "" {
		return ""
	}

	// Try to parse as JSON to sanitize sensitive fields
	var data map[string]interface{}
	if err := json.Unmarshal([]byte(payload), &data); err != nil {
		// Not valid JSON, return as-is (truncated if needed)
		if len(payload) > 10000 {
			return payload[:10000] + "...[truncated]"
		}
		return payload
	}

	// Sanitize sensitive keys
	sanitizeMap(data, config.SensitiveKeys)

	sanitized, err := json.Marshal(data)
	if err != nil {
		return payload
	}

	return string(sanitized)
}

func sanitizeMap(data map[string]interface{}, sensitiveKeys []string) {
	for k, v := range data {
		lowerKey := strings.ToLower(k)
		for _, sk := range sensitiveKeys {
			if strings.Contains(lowerKey, sk) {
				data[k] = "[REDACTED]"
				break
			}
		}

		if nested, ok := v.(map[string]interface{}); ok {
			sanitizeMap(nested, sensitiveKeys)
		}
	}
}

// responseRecorder wraps http.ResponseWriter to capture response data
type responseRecorder struct {
	http.ResponseWriter
	statusCode int
	body       *bytes.Buffer
	wroteHeader bool
}

func (rr *responseRecorder) WriteHeader(code int) {
	if !rr.wroteHeader {
		rr.statusCode = code
		rr.wroteHeader = true
		rr.ResponseWriter.WriteHeader(code)
	}
}

func (rr *responseRecorder) Write(p []byte) (n int, err error) {
	if !rr.wroteHeader {
		rr.WriteHeader(http.StatusOK)
	}
	rr.body.Write(p)
	return rr.ResponseWriter.Write(p)
}

func (rr *responseRecorder) Header() http.Header {
	return rr.ResponseWriter.Header()
}

// Gunzip decompresses gzip data
func gunzip(data []byte) ([]byte, error) {
	reader, err := gzip.NewReader(bytes.NewReader(data))
	if err != nil {
		return nil, err
	}
	defer reader.Close()
	return io.ReadAll(reader)
}

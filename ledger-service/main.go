package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strings"

	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
)

func initTracer() func(context.Context) error {
	collectorURL := os.Getenv("TRACEFLOW_COLLECTOR_URL")
	if collectorURL == "" {
		collectorURL = "http://localhost:4317"
	}
	apiKey := os.Getenv("TRACEFLOW_API_KEY")
	headers := map[string]string{}
	if apiKey != "" {
		headers["x-api-key"] = apiKey
	}

	exp, err := otlptracehttp.New(context.Background(),
		otlptracehttp.WithEndpoint(strings.TrimPrefix(strings.TrimPrefix(collectorURL, "https://"), "http://")),
		otlptracehttp.WithHeaders(headers),
		otlptracehttp.WithInsecure(),
	)
	if err != nil {
		log.Printf("[TraceFlow] Failed to create OTLP exporter: %v", err)
		return func(ctx context.Context) error { return nil }
	}

	res := resource.NewWithAttributes(
		semconv.SchemaURL,
		semconv.ServiceName("ledger-service"),
	)

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exp),
		sdktrace.WithResource(res),
	)
	otel.SetTracerProvider(tp)
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{},
		propagation.Baggage{},
	))

	log.Println("[TraceFlow] OpenTelemetry tracer initialized")
	return tp.Shutdown
}

type LedgerRecord struct {
	PaymentID string  `json:"paymentId"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
}

func recordHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var record LedgerRecord
	if err := json.NewDecoder(r.Body).Decode(&record); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	log.Printf("Recording payment %s to immutable ledger", record.PaymentID)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"status": "recorded"})
}

func main() {
	shutdown := initTracer()
	defer shutdown(context.Background())

	mux := http.NewServeMux()
	mux.HandleFunc("/ledger/record", recordHandler)

	// otelhttp wraps the entire mux — zero changes to handlers
	handler := otelhttp.NewHandler(mux, "ledger-service")

	port := os.Getenv("PORT")
	if port == "" {
		port = "8083"
	}

	log.Printf("Ledger Service running on port %s", port)
	if err := http.ListenAndServe(":"+port, handler); err != nil {
		log.Fatal(err)
	}
}

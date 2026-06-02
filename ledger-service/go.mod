module ledger-service

go 1.21

require (
	go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp v0.53.0
	go.opentelemetry.io/otel v1.28.0
	go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp v1.28.0
	go.opentelemetry.io/otel/sdk v1.28.0
)

package com.payflow.commons.traceflow;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TraceFlowLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private String collectorUrl = "http://localhost:4317";
    private String workspaceId = "ws_dev";
    private HttpClient httpClient;
    private ExecutorService executor;

    @Override
    public void start() {
        httpClient = HttpClient.newHttpClient();
        executor = Executors.newSingleThreadExecutor();
        super.start();
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) return;
        
        // Only send WARN and ERROR logs
        if (event.getLevel().levelInt < 30) return; // WARN=30, ERROR=40

        executor.submit(() -> {
            try {
                String traceId = MDC.get("trace_id");
                String spanId = MDC.get("span_id");
                
                if (traceId == null || traceId.isEmpty()) {
                    return;
                }

                Map<String, Object> span = new HashMap<>();
                span.put("id", UUID.randomUUID().toString());
                span.put("trace_id", traceId);
                if (spanId != null && !spanId.isEmpty()) {
                    span.put("parent_id", spanId);
                }
                span.put("service_name", MDC.get("service_name") != null ? MDC.get("service_name") : "unknown");
                span.put("operation_name", "log/" + event.getLevel().toString().toLowerCase());
                span.put("kind", "internal");
                span.put("started_at", Instant.now().toString());
                span.put("ended_at", Instant.now().toString());
                span.put("duration_ms", 0);
                span.put("status", event.getLevel().levelInt >= 40 ? "error" : "ok");
                
                Map<String, String> tags = new HashMap<>();
                tags.put("log.level", event.getLevel().toString());
                tags.put("log.logger", event.getLoggerName());
                tags.put("log.message", event.getFormattedMessage());
                if (event.getThrowableProxy() != null) {
                    tags.put("log.exception", event.getThrowableProxy().getClassName());
                }
                span.put("tags", tags);
                span.put("workspace_id", workspaceId);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(collectorUrl + "/spans"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(span)))
                    .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                // Silently fail to avoid log loops
            }
        });
    }

    public void setCollectorUrl(String collectorUrl) {
        this.collectorUrl = collectorUrl;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                sb.append("{");
                boolean firstInner = true;
                for (Map.Entry<String, String> inner : ((Map<String, String>) value).entrySet()) {
                    if (!firstInner) sb.append(",");
                    firstInner = false;
                    sb.append("\"").append(inner.getKey()).append("\":\"").append(escapeJson(inner.getValue())).append("\"");
                }
                sb.append("}");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

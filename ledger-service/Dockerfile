FROM golang:1.21-alpine AS builder

WORKDIR /app/ledger-service

COPY ledger-service/go.mod .

# Let Go resolve all transitive dependencies automatically
RUN go mod download || true

COPY ledger-service/ .

# go mod tidy will fix any missing/mismatched dependencies
RUN go mod tidy
RUN go build -o ledger-service main.go

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/ledger-service/ledger-service .

EXPOSE 8083
CMD ["./ledger-service"]

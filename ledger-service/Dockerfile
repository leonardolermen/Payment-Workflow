FROM golang:1.21-alpine AS builder

WORKDIR /app
# Copy the local SDK into the build context
COPY sdks/sdk-go ./sdk-go
COPY ledger-service ./ledger-service

WORKDIR /app/ledger-service
RUN go mod download
RUN go build -o ledger-service main.go

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /app/ledger-service/ledger-service .

EXPOSE 8083
CMD ["./ledger-service"]

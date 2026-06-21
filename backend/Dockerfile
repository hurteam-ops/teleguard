# =============================================================================
# TeleFlow Backend — Multi-stage Docker Build
# =============================================================================

# Stage 1: Build
FROM golang:1.22-alpine AS builder

WORKDIR /app

RUN apk add --no-cache git ca-certificates

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" -o /app/teleflow-api ./cmd/api

# Stage 2: Run
FROM alpine:3.19

RUN apk add --no-cache ca-certificates tzdata

WORKDIR /app

COPY --from=builder /app/teleflow-api .

# Security: run as non-root
RUN addgroup -S teleflow && adduser -S teleflow -G teleflow
USER teleflow

EXPOSE 8080

ENTRYPOINT ["./teleflow-api"]

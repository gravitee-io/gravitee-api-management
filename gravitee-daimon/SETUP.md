# DAImon POC - Setup Guide

## Prerequisites

- Go 1.23+
- Gravitee APIM running locally (gateway on :8082, console on :8083)

## 1. Build the DAImon

```bash
cd gravitee-daimon
go build -o daimon ./cmd/daimon/
```

## 2. Import the AI API in APIM

1. Open Gravitee Console (http://localhost:8083)
2. Go to APIs → Import → Import file
3. Select `apim-api-definition.json`
4. Start the API

This creates an API on `/ai/v1/` that proxies to `https://api.anthropic.com/v1/`.

## 3. Start the DAImon

```bash
./daimon
```

The TUI dashboard appears. The DAImon:
- Listens on `:8990` (local proxy)
- Registers with the Gateway on `:8082`
- Sends heartbeats every 30s
- Scans for direct AI connections every 10s

## 4. Configure Claude Code to use the DAImon

```bash
export ANTHROPIC_BASE_URL=http://localhost:8990
```

All Claude Code requests now flow through: Claude Code → DAImon → Gateway → Anthropic.

## 5. Demo scenarios

### Hot-reload policies
Edit `policies.yaml` while the DAImon is running. Changes apply instantly (visible in TUI).

### Test secret detection
Send a prompt containing `AKIA1234567890ABCDEF` — the DAImon blocks it.

### View metrics
```bash
cat ~/.daimon/metrics.jsonl | jq .
```

### List registered devices (via Gateway management API)
```bash
curl http://localhost:18082/daimon/devices
```
(18082 is the default Gateway management port)

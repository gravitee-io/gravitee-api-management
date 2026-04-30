# Gravitee DAImon - Design Document

## Overview

**DAImon** (Daemon + AI + δαίμων, the guiding spirit in Greek) is a lightweight local agent that sits between AI tools (Claude Code, Cursor, Gemini CLI, etc.) and the Gravitee AI Gateway. It intercepts AI traffic, enforces local policies before requests leave the machine, collects metrics, and registers with the Gateway for centralized fleet management.

## Problem Statement

Organizations using Gravitee's AI Gateway want to centralize all AI traffic for audit, cost monitoring, DLP, and policy enforcement. However, AI developer tools (CLI, IDE extensions, desktop apps) talk directly to providers (api.anthropic.com, api.openai.com). There is no standard way to:

1. **Route** this traffic through the corporate AI Gateway
2. **Enforce policies locally** before sensitive data leaves the laptop (secrets, PII, proprietary code)
3. **Monitor** which tools and models are being used, by whom, and at what cost
4. **Detect** tools that bypass the gateway entirely

## Architecture

### Traffic Flow

```
AI Tool (Claude Code, etc.)
    │
    │  ANTHROPIC_BASE_URL=http://localhost:8990
    ▼
┌─────────────────────────────────┐
│         DAImon (:8990)          │
│                                 │
│  ┌───────────┐  ┌────────────┐  │
│  │  Policy    │  │  Metrics   │  │
│  │  Engine    │  │  Collector │  │
│  └─────┬─────┘  └─────┬──────┘  │
│        │               │        │
│  ┌─────▼───────────────▼──────┐ │
│  │      HTTP Reverse Proxy    │ │
│  └─────────────┬──────────────┘ │
│                │                │
│  ┌─────────────▼──────────────┐ │
│  │  Registration & Heartbeat  │ │
│  └─────────────┬──────────────┘ │
│                │                │
│  ┌─────────────▼──────────────┐ │
│  │  Direct Connection Detector│ │
│  └────────────────────────────┘ │
│                                 │
│  ┌────────────────────────────┐ │
│  │  TUI Dashboard (bubbletea) │ │
│  └────────────────────────────┘ │
└────────────────┬────────────────┘
                 │
                 ▼
┌────────────────────────────────┐
│   Gravitee AI Gateway (:8082)  │
│                                │
│  ┌──────────┐  ┌─────────────┐ │
│  │ AI API   │  │ Control API │ │
│  │ (proxy)  │  │ (register,  │ │
│  │          │  │  heartbeat, │ │
│  │          │  │  config)    │ │
│  └────┬─────┘  └─────────────┘ │
└───────┼────────────────────────┘
        │
        ▼
  api.anthropic.com
```

### Components

| Component | Responsibility |
|-----------|---------------|
| **HTTP Reverse Proxy** | Forwards AI requests to the Gateway, injects headers (device ID, user) |
| **Policy Engine** | Loads policies from YAML, hot-reloads on file change, evaluates request/response |
| **Direct Connection Detector** | Scans for processes connecting directly to AI providers (lsof/netstat) |
| **Registration & Heartbeat** | Registers with the Gateway at startup, periodic heartbeat, fetches config |
| **Metrics Collector** | Captures per-request metrics (tokens, model, latency, cost), writes to JSONL file |
| **TUI Dashboard** | Real-time terminal UI showing traffic, metrics, policy actions, connection status |

## Technology Choice: Go

### Why not Rust?

Rust would be the ideal choice for a production daemon: smaller binary, lower memory footprint, no GC pauses, and better suited for long-running system-level processes. For a production release, Rust should be strongly considered.

### Why Go for this POC?

- **Prototyping speed**: Go's standard library includes a production-grade reverse proxy (`net/http/httputil.ReverseProxy`) out of the box, reducing the proxy implementation to a few lines
- **Hot-reload ecosystem**: `fsnotify` for file watching, `gopkg.in/yaml.v3` for parsing — mature, well-tested libraries
- **TUI**: `bubbletea` (Charm) is the most mature terminal UI framework available in any language
- **Concurrency model**: goroutines make it trivial to run the proxy, detector, heartbeat, and TUI concurrently
- **Cross-compilation**: `GOOS=windows go build` produces a Windows binary from macOS — useful for demonstrating cross-platform support
- **Sufficient performance**: for a local proxy handling a few requests per minute, Go's overhead is negligible

## Policy Engine

### Design Principles

- **Declarative policies**: defined in YAML, not compiled into the binary
- **Hot-reloadable**: file system watcher detects changes, reloads without restart
- **Pre-send evaluation**: policies run before the request leaves the machine, offloading the gateway and network
- **Chain execution**: policies execute in order, first `block` action wins

### Policy Configuration

```yaml
policies:
  - name: secret-detection
    enabled: true
    type: content-filter
    action: block
    config:
      patterns:
        - name: aws-access-key
          regex: "AKIA[0-9A-Z]{16}"
        - name: openai-api-key
          regex: "sk-[a-zA-Z0-9]{48}"
        - name: github-token
          regex: "ghp_[a-zA-Z0-9]{36}"
        - name: generic-secret
          regex: "(?i)(password|secret|token)\\s*[:=]\\s*['\"][^'\"]{8,}"
      message: "Blocked: sensitive content detected ({{.MatchName}})"

  - name: token-budget
    enabled: true
    type: token-limit
    action: block
    config:
      max_tokens_per_request: 100000
      estimation_method: whitespace
      message: "Blocked: estimated {{.EstimatedTokens}} tokens exceeds limit of {{.MaxTokens}}"

  - name: allowed-models
    enabled: true
    type: model-allowlist
    action: block
    config:
      models:
        - claude-sonnet-4-20250514
        - claude-haiku-4-5-20251001
      message: "Blocked: model {{.RequestedModel}} is not in the allowlist"
```

### Policy Interface

Each policy implements a simple interface:

```go
type Policy interface {
    Name() string
    Evaluate(ctx context.Context, req *InterceptedRequest) *PolicyResult
}

type PolicyResult struct {
    Action  Action // Allow, Block, Warn
    Reason  string
    Details map[string]any
}
```

### Hot-Reload Flow

1. `fsnotify` watches `policies.yaml`
2. On change event, parse new YAML
3. Validate policy definitions
4. Atomically swap the policy chain (sync.RWMutex)
5. Log reload event to TUI and metrics

## Registration & Heartbeat

The DAImon registers with the Gateway through a **control API** exposed as endpoints in the Gravitee Gateway (Java).

### Registration Flow

```
DAImon startup
    │
    ├─ Generate device ID (from hardware: MAC address + hostname hash)
    │
    ├─ POST /daimon/register
    │   Body: {
    │     "deviceId": "d-a1b2c3d4",
    │     "hostname": "yann-macbook",
    │     "user": "yann.tavernier",
    │     "version": "0.1.0",
    │     "os": "darwin/arm64",
    │     "capabilities": ["proxy", "policy-engine", "detector"]
    │   }
    │   Response: {
    │     "status": "registered",
    │     "configVersion": 1,
    │     "heartbeatIntervalSec": 30
    │   }
    │
    ├─ GET /daimon/config
    │   Response: policies YAML (or JSON) to apply
    │   (For POC: returns static config; in production, per-device/per-team config)
    │
    └─ Start heartbeat loop
        │
        every 30s ──► POST /daimon/heartbeat
                       Body: {
                         "deviceId": "d-a1b2c3d4",
                         "timestamp": "2026-04-30T10:00:00Z",
                         "uptime_sec": 3600,
                         "stats": {
                           "requests_total": 42,
                           "requests_blocked": 3,
                           "tokens_total": 125000
                         }
                       }
```

### Control API (Java, Gateway-side)

The control API is implemented as Java endpoints within the Gravitee Gateway. For the POC, it provides:

- `POST /daimon/register` — stores device info in memory (ConcurrentHashMap)
- `GET /daimon/config` — returns a static policy configuration
- `POST /daimon/heartbeat` — updates last-seen timestamp, stores stats
- `GET /daimon/devices` — lists all registered devices (for monitoring/demo)

## Metrics

### Collection

Every proxied request generates a metric event with:

| Field | Description |
|-------|-------------|
| `timestamp` | ISO 8601 timestamp |
| `type` | `request`, `policy_block`, `policy_warn`, `direct_connection` |
| `device_id` | DAImon device identifier |
| `model` | AI model requested |
| `tokens_in` | Input token count (estimated or from response) |
| `tokens_out` | Output token count (from response) |
| `cost_usd` | Estimated cost based on model pricing |
| `latency_ms` | Round-trip latency |
| `policy_applied` | Policy name that acted on this request |
| `action` | `allowed`, `blocked`, `warned` |
| `tool` | Source tool if detectable (from User-Agent) |

### Storage (POC)

Metrics are appended to a local JSONL file (`~/.daimon/metrics.jsonl`), one event per line:

```json
{"timestamp":"2026-04-30T10:15:00Z","type":"request","device_id":"d-a1b2c3d4","model":"claude-sonnet-4-20250514","tokens_in":1200,"tokens_out":450,"cost_usd":0.0087,"latency_ms":1230,"policy_applied":"secret-detection","action":"allowed","tool":"claude-code"}
{"timestamp":"2026-04-30T10:15:30Z","type":"policy_block","device_id":"d-a1b2c3d4","model":"claude-sonnet-4-20250514","tokens_in":800,"tokens_out":0,"cost_usd":0,"latency_ms":2,"policy_applied":"secret-detection","action":"blocked","tool":"claude-code"}
```

In production, this would be replaced by Elasticsearch or the Gravitee Analytics backend.

## Direct Connection Detector

Periodically scans for processes making direct TCP connections to known AI provider endpoints:

- `api.anthropic.com`
- `api.openai.com`
- `generativelanguage.googleapis.com`

Uses `lsof -i -n -P` (macOS/Linux) to detect established connections. When a direct connection is found:

1. Log a `direct_connection` metric event
2. Display a warning in the TUI
3. Optionally identify the process name and PID

## TUI Dashboard

Built with [bubbletea](https://github.com/charmbracelet/bubbletea) (Charm), the TUI provides a real-time view:

```
┌─ Gravitee DAImon v0.1.0 ────────────── ● Connected to Gateway ─┐
│                                                                  │
│  Traffic                                                         │
│  ──────────────────────────────────────────────────────────────── │
│  10:15:00  ✓ claude-sonnet-4  1,200→450 tok  $0.009  1.2s       │
│  10:15:30  ✗ claude-sonnet-4  BLOCKED: AWS key detected          │
│  10:16:12  ✓ claude-sonnet-4  3,400→1,200 tok  $0.024  2.1s     │
│  10:16:45  ⚠ DIRECT CONNECTION: cursor (PID 1234) → anthropic   │
│                                                                  │
│  Metrics                          Policies                       │
│  ────────────────────────         ────────────────────────        │
│  Requests:     42                 secret-detection:  ● active    │
│  Blocked:       3                 token-budget:      ● active    │
│  Tokens in:   125,000             allowed-models:    ● active    │
│  Tokens out:   45,000                                            │
│  Est. cost:    $1.23              Last reload: 10:14:00           │
│  Uptime:      1h 23m                                             │
│                                                                  │
│  [q] quit  [p] policies  [d] devices  [m] metrics               │
└──────────────────────────────────────────────────────────────────┘
```

## AI API in APIM

A Gravitee APIM API definition that proxies to Anthropic:

- **Entrypoint**: HTTP Proxy
- **Backend**: `https://api.anthropic.com`
- **Path**: `/v1/messages` (and other Anthropic endpoints)
- **Policies**: the Gateway can apply its own policies (rate limiting, analytics, etc.) on top of the DAImon's local policies
- **Authentication**: API key passthrough (the DAImon forwards the user's Anthropic API key)

## Project Structure

```
gravitee-daimon/
├── DESIGN.md                     # This document
├── cmd/
│   └── daimon/
│       └── main.go               # Entrypoint: wires all components, starts proxy + TUI
├── internal/
│   ├── proxy/
│   │   └── proxy.go              # HTTP reverse proxy with middleware chain
│   ├── policy/
│   │   ├── engine.go             # Policy loader, hot-reload, chain evaluator
│   │   ├── policy.go             # Policy interface and types
│   │   ├── content_filter.go     # Secret/sensitive content detection
│   │   ├── token_limit.go        # Token budget enforcement
│   │   └── model_allowlist.go    # Model whitelist
│   ├── detector/
│   │   └── detector.go           # Direct connection scanner (lsof/netstat)
│   ├── registration/
│   │   └── registration.go       # Gateway registration + heartbeat + config fetch
│   ├── metrics/
│   │   └── metrics.go            # Metric event collection, JSONL writer
│   └── tui/
│       └── tui.go                # Bubbletea TUI dashboard
├── config.yaml                   # DAImon configuration (gateway URL, device settings)
├── policies.yaml                 # Policy definitions (hot-reloadable)
└── go.mod
```

## Demo Scenario

1. **Start the DAImon** — it registers with the Gateway, TUI appears showing connection status
2. **Make a Claude Code request** routed through the DAImon — traffic appears in real-time in the TUI with token counts and cost
3. **Edit `policies.yaml` live** — add a secret detection pattern — the DAImon hot-reloads instantly (visible in TUI)
4. **Send a prompt containing an AWS key** — blocked by the policy, TUI shows the block with reason
5. **Show direct connection detection** — an unconfigured tool calls Anthropic directly — warning appears in TUI
6. **Show metrics file** — `cat ~/.daimon/metrics.jsonl` shows all captured events

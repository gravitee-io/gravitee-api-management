# Gravitee DAImon — Product Requirements Document

**A note on naming**: Throughout this document, the control plane module is referred to as "AI Fleet" for consistency. This name is not final. Alternatives under consideration include "AI Devices", "Anchor", or other options. The name "AI Fleet" was used in the POC and may evolve as the product positioning within Gamma / Agent Management is finalized.

## 1. Problem Statement

Organizations using AI developer tools (Claude Code, Cursor, Gemini CLI, GitHub Copilot) face a fundamental visibility and control gap: these tools communicate directly with AI provider APIs over HTTPS, bypassing any corporate infrastructure.

There is currently no standard way to:

- Route AI tool traffic through a corporate AI Gateway for audit and cost monitoring
- Enforce data loss prevention (DLP) policies locally, before sensitive data leaves the developer's machine
- Monitor which tools, models, and prompts are being used across the organization
- Detect tools that bypass corporate controls entirely by connecting directly to AI providers

DAImon (Daemon + AI + δαίμων, the guiding spirit in Greek) is a lightweight local agent that bridges this gap. It sits between AI tools and the Gravitee AI Gateway, intercepting traffic, enforcing policies, collecting metrics, and reporting to a centralized fleet management console.

## 2. Target Features

### 2.1. Administrator Features (AI Fleet Control Plane)

These features are exposed in the Gamma console and target IT/security administrators managing the fleet:

- **Fleet overview**: View all registered DAImon instances, their status (active/inactive), OS, version, last heartbeat
- **Metrics dashboard**: Token usage, request volume, blocked requests, cost estimation — aggregated across the fleet, filterable by device/team/model
- **Shadow AI visibility**: See which tools and processes are connecting directly to AI providers without going through DAImon, with trends over time (managed vs unmanaged traffic ratio)
- **Policy management**: Create, edit, and push policy configurations to the fleet from the console. Track rollout status (which devices have applied the latest version)
- **Configuration management**: Edit the DAImon configuration (AI provider detection list, upstream Gateway URL, behavioral settings) from the console. Push configuration changes to individual devices or device groups
- **AI provider detection list**: Configure which AI provider endpoints are monitored for shadow AI detection. Includes sensible defaults (Anthropic, OpenAI, Google) with the ability to add custom endpoints (Azure OpenAI, internal model servers)
- **Alerts and notifications**: Notify administrators when shadow AI is detected, when devices go offline, or when policy violations spike

### 2.2. DAImon Features (On-Device Agent)

These features run on the developer's machine. DAImon is transparent to the end user — it operates silently in the background once deployed via MDM:

- **Local policy enforcement**: Evaluate requests against policies (secret detection, token budget, model allowlist) before they leave the machine. Block or warn on violation.
- **Shadow AI detection**: Scan local network connections to identify processes communicating with known AI provider endpoints without going through DAImon. Report findings to the Gateway.
- **Traffic forwarding**: Act as a reverse proxy — receive AI tool requests and forward them to the AI Gateway after policy evaluation
- **Metrics collection and reporting**: Capture per-request metrics (model, tokens, latency, tool identity) and push them to the Gateway for centralized storage and analysis
- **Configuration hot-reload**: Watch configuration and policy files for changes. Apply updates without restart.
- **Self-registration and heartbeat**: Register with the Gateway on startup, send periodic heartbeats with health status and aggregated stats
- **Tool identification**: Infer which AI tool is making each request (via User-Agent header, process detection, or other heuristics) to provide per-tool analytics and per-tool policy enforcement
- **Debug/troubleshoot mode (TUI)**: An optional terminal UI for operators to inspect live traffic, policy decisions, and connection status locally. Not intended for end users in normal MDM deployment — useful for setup, debugging, and troubleshooting.

## 3. Solution Architecture

### 3.1. Without DAImon

AI tools connect directly to provider APIs (api.anthropic.com, api.openai.com). The organization has zero visibility into what models are used, what data is sent, or what costs are incurred.

See [diagram-without-daimon.md](diagram-without-daimon.md).

### 3.2. With DAImon

See [diagram-with-daimon.md](diagram-with-daimon.md) and [diagram-internal-architecture.md](diagram-internal-architecture.md).

DAImon introduces two layers of control:

**Passive observation (network scanning) — Shadow AI Detection**

DAImon continuously scans local network connections using OS-specific mechanisms (`lsof` on macOS/Linux, `netstat` or Windows APIs on Windows) to detect any process communicating with known AI provider endpoints. This runs regardless of whether traffic is routed through DAImon. Its mission is to surface shadow AI usage: tools and processes that bypass corporate controls.

This does not intercept or modify traffic. It only observes and reports. The list of monitored AI provider endpoints is configurable (see Section 3.3).

Shadow AI detection is a first-class feature, not just a side effect of the proxy. Even if an organization cannot immediately enforce routing through DAImon (e.g., because a tool does not support base URL overrides), passive observation provides visibility into which tools are being used, which providers are being contacted, and how often. This data is critical for security teams to assess risk and prioritize enforcement efforts.

In the Events page of the control plane, shadow AI detections are surfaced alongside proxied traffic, giving operators a unified view of all AI activity on the machine — both managed and unmanaged.

**Important nuance**: Shadow AI detection must distinguish between intentional bypass and expected tool behavior. For example, Claude Code makes direct calls to api.anthropic.com for internal operations (telemetry, lightweight Haiku classification) even when `ANTHROPIC_BASE_URL` is set. These are not malicious bypasses — they are architectural characteristics of the tool. The system should allow administrators to classify known direct connections as "expected" vs "suspicious" to reduce noise and focus attention on genuine shadow AI usage.

**Active interception (HTTP proxy)**

When an AI tool is configured to route through DAImon (e.g., `ANTHROPIC_BASE_URL=http://localhost:8990`), DAImon acts as a reverse proxy. It receives the full request in plaintext, applies local policies (secret detection, token budget, model allowlist), and either blocks the request or forwards it to the AI Gateway.

Why this approach:

- **Non-invasive**: No system-level network hooks, no certificate injection, no root/admin privileges required. A single environment variable redirects traffic.
- **Declarative**: The configuration is explicit. The developer and the organization both know exactly what is happening.
- **Portable**: Works identically on macOS, Linux, and Windows. No OS-specific implementation.
- **Flexible**: Can be configured per-tool. Tools that don't support base URL overrides can still be observed passively.
- **Debuggable**: Standard HTTP proxy — easy to inspect, test, and troubleshoot.

The alternative — transparent interception via OS-level traffic redirection (iptables, pf) — was evaluated and rejected. It requires:

- Root/admin privileges
- TLS MITM with custom CA injection into the system trust store
- OS-specific implementations (iptables on Linux, pf on macOS, WinDivert on Windows)
- Significantly more complexity to debug and maintain
- Some tools (e.g., Cursor) implement certificate pinning, which would reject forged certificates entirely

The explicit proxy approach avoids all of these issues.

### 3.3. DAImon Configuration and Provider Management

DAImon's behavior is controlled by two configuration files:

- **`config.yaml`**: DAImon instance settings — Gateway URL, device identity, AI provider list for passive detection, upstream forwarding target
- **`policies.yaml`**: Policy definitions — secret detection patterns, token budgets, model allowlists

Both files should be manageable from the AI Fleet control plane UI, not just locally on the machine. In the POC, the Policies page already supports editing `policies.yaml` from the UI with live reload. The same approach should be extended to `config.yaml`.

**AI Provider List**

The provider list defines which AI endpoints DAImon monitors for shadow AI detection. The POC ships with defaults (api.anthropic.com, api.openai.com), but organizations must be able to add custom AI provider endpoints (e.g., internal model serving endpoints, Azure OpenAI instances, self-hosted LLMs).

This configuration is part of `config.yaml`:

```yaml
detector:
  providers:
    - api.anthropic.com
    - api.openai.com
    - generativelanguage.googleapis.com
    - my-internal-llm.corp.example.com
```

**Group-Based Configuration**

In a production fleet, different teams may require different configurations. For example, the engineering team may have access to more powerful models, while the finance team is restricted to smaller models with stricter DLP policies. Group-based configuration assignment (per-team or per-device-group) should be considered for the roadmap but is out of scope for the initial release.

### 3.4. Traffic Flow

See [diagram-request-lifecycle.md](diagram-request-lifecycle.md).

```
AI Tool → DAImon (local policy enforcement) → AI Gateway (server-side policies) → AI Provider
```

DAImon forwards to the AI Gateway, not directly to the provider. This enables a defense-in-depth model:

- **Client-side** (DAImon): DLP, secret detection, token budget — runs before data leaves the machine
- **Server-side** (Gateway): rate limiting, audit logging, cost allocation, global model restrictions

### 3.5. TLS Considerations

This aspect has not been implemented in the POC but is critical for production.

Since DAImon will be deployed and managed via MDM (e.g., Kandji, Jamf), certificate management becomes feasible:

- The MDM can provision a client certificate to each DAImon instance
- DAImon authenticates to the AI Gateway via mTLS (mutual TLS)
- The Gateway verifies the client certificate, ensuring only authorized DAImon instances can forward traffic
- This also prevents direct API access to the Gateway without a valid DAImon certificate

**This is a key area to explore further.** See [Section 10 — Open Questions](#10-open-questions).

### 3.6. Integration with Gamma API Types

Gravitee Gamma currently supports three API types relevant to AI traffic management. Understanding how DAImon integrates with each is important for the product roadmap.

**LLM Proxy**

The LLM Proxy accepts OpenAI-compatible API requests and translates them to provider-specific formats (Gemini, AWS Bedrock, OpenAI). It provides a unified interface with features like semantic caching, token usage tracking, and tool/function calling support.

In the POC, DAImon forwards traffic to a simple HTTP Proxy API in the Gateway. For production, DAImon should forward to an LLM Proxy API instead, which would give the Gateway full visibility into the AI request semantics (model, tokens, cost).

**Current limitation**: The LLM Proxy only accepts OpenAI-compatible input format. DAImon currently forwards raw Anthropic-format requests (since the POC targets Claude Code). This creates a format mismatch: either DAImon must translate Anthropic requests to OpenAI format before forwarding, or the LLM Proxy must be extended to accept Anthropic-native input. This is a key architectural decision that requires further analysis. See [Section 10 — Open Questions](#10-open-questions).

**A2A Proxy (Agent-to-Agent)**

The A2A Proxy enables structured, secure, and observable communication between AI agents. It supports both synchronous and asynchronous patterns, with full policy support (rate limiting, RBAC, guardrails).

DAImon could potentially leverage A2A for its own communication with the Gateway (registration, heartbeat, config sync), treating the DAImon-Gateway relationship as an agent-to-agent interaction. This is a future consideration, not a requirement for the initial release.

**MCP Server (Model Context Protocol)**

Gravitee can convert existing REST APIs into MCP servers, allowing AI agents to discover and invoke them as tools. AI tools like Claude Code already consume MCP servers — they connect to MCP endpoints to discover and call tools during their workflow.

In the DAImon context, MCP traffic is another category of AI-related network activity that organizations may want to observe and govern. If an AI tool connects to an MCP server through the Gateway, standard Gateway policies (rate limiting, RBAC, audit) apply. If it connects directly, the shadow AI scanner can detect the connection just like any other direct AI provider call.

This is a future integration opportunity, not a requirement for the initial release.

## 4. Technology Choice: Go

### 4.1. Go vs Rust Comparison

Both Go and Rust were evaluated for DAImon. The comparison focuses on the features DAImon actually needs:

| Feature | Go | Rust | Winner |
|---|---|---|---|
| Reverse proxy | Built-in (`net/http/httputil.ReverseProxy`), production-grade, ~10 LOC | No built-in equivalent. Requires assembling `hyper`/`axum` + middleware | Go |
| File watching (hot-reload) | `fsnotify` — mature, simple API | `notify` crate — comparable quality | Tie |
| YAML parsing | `gopkg.in/yaml.v3` — battle-tested | `serde_yaml` — excellent, type-safe | Tie |
| TUI framework | `bubbletea` (Charm) — mature, elegant MVU pattern | `ratatui` — mature, feature-rich | Tie |
| Concurrency | Goroutines — trivial to spawn, channels for coordination | `tokio` async/await — more control, borrow checker prevents data races | Go (simpler) |
| Cross-compilation | `GOOS=windows go build` — seamless from any platform | Requires per-target toolchain setup, `cross` tool | Go |
| GC pauses | ~10-30ms (negligible vs network latency of 10-50ms+) | None (zero-cost) | Rust (theoretical) |
| Memory footprint | ~50-100MB baseline | ~5-15MB baseline | Rust (theoretical) |
| Startup time | ~10ms | ~1-2ms | Rust (theoretical) |

Go wins on every feature that matters operationally. Rust's advantages (no GC, lower memory, faster startup) are theoretical for DAImon's workload:

- **Memory**: 50MB vs 15MB is 0.6% of RAM on a modern machine. DAImon runs on independent user machines, not on a shared server where memory is pooled. There is no aggregation effect.
- **GC pauses**: 10-30ms is invisible compared to TLS handshake latency (100-200ms) and network round-trip (10-50ms).
- **Startup time**: DAImon is a long-running daemon. Startup cost is paid once and is irrelevant.

### 4.2. Cross-Platform Build

Go's native cross-compilation is a significant advantage for MDM deployment:

```
GOOS=darwin  GOARCH=arm64 go build -o daimon-macos-arm64
GOOS=darwin  GOARCH=amd64 go build -o daimon-macos-amd64
GOOS=linux   GOARCH=amd64 go build -o daimon-linux-amd64
GOOS=windows GOARCH=amd64 go build -o daimon-windows-amd64.exe
```

A single CI pipeline produces binaries for all targets. No per-platform toolchain, no linking issues.

### 4.3. Platform-Specific Considerations

The direct connection detector (shadow AI scanner) must use OS-specific mechanisms to scan for TCP connections to known AI provider endpoints:

| Platform | Mechanism | Notes |
|----------|-----------|-------|
| macOS / Linux | `lsof -i -n -P` | Used in the POC. Mature, well-understood. |
| Windows | `netstat -ano`, `Get-NetTCPConnection` (PowerShell), or `GetExtendedTcpTable` (Win32 API) | Not yet implemented. Required before Windows deployment. |

The scanner abstraction should hide OS-specific details behind a common interface, making it straightforward to add new platforms.

### 4.4. Conclusion

Go is the pragmatic choice for DAImon — not just for the POC, but for production. It wins on every operational dimension and its theoretical disadvantages have no practical impact on a local proxy daemon.

## 5. Policy Engine

### 5.1. Current State (POC)

Policies are implemented in Go and configured via YAML:

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
        - name: generic-secret
          regex: "(?i)(password|secret|token)\\s*[:=]\\s*['\"]?[^'\"\\s]+"
      message: "Blocked: sensitive content detected ({{.MatchName}})"

  - name: token-budget
    enabled: true
    type: token-limit
    action: block
    config:
      max_tokens_per_request: 100000
      message: "Blocked: estimated {{.EstimatedTokens}} tokens exceeds limit"

  - name: allowed-models
    enabled: true
    type: model-allowlist
    action: block
    config:
      models:
        - claude-sonnet-4-20250514
        - claude-haiku-4-5-20251001
      message: "Blocked: model {{.RequestedModel}} is not allowed"
```

Policies are hot-reloadable: `fsnotify` watches `policies.yaml`, parses changes, and atomically swaps the policy chain (via `sync.RWMutex`). No restart required. See [diagram-policy-hotreload.md](diagram-policy-hotreload.md).

The YAML configuration is declarative and flexible: enabling/disabling a policy, changing thresholds or regex patterns, or reordering the chain is a file edit.

### 5.2. Limitation

Adding a new **type** of policy (e.g., PII detection, prompt injection detection, cost estimation) requires writing Go code and rebuilding the binary. The YAML only configures existing policy types.

### 5.3. Future Direction: Runtime-Loadable Policies

To allow new policy logic to be deployed without rebuilding DAImon, two approaches were evaluated:

**CEL (Common Expression Language)**

- Lightweight expression language created by Google
- Used in production by Google Cloud IAM, Kubernetes, Envoy Proxy
- No side effects by design — safe to execute untrusted expressions
- Excellent Go integration (`github.com/google/cel-go`)
- Well-suited for simple policies: pattern matching, threshold checks, allowlists

Example:

```
// secret-detection
request.body.matches("AKIA[0-9A-Z]{16}") || request.body.matches("(?i)password\\s*[:=]")

// token-budget
request.estimated_tokens <= 100000

// allowed-models
request.model in ["claude-sonnet-4-20250514", "claude-haiku-4-5-20251001"]
```

**Lua**

- Full scripting language, lightweight runtime
- Used in production by Nginx (OpenResty), Redis, HAProxy
- More expressive than CEL: loops, tables, functions, string manipulation
- Good Go integration (`github.com/yuin/gopher-lua`)
- Well-suited for complex policies: multi-step evaluation, stateful checks, custom logic

Example:

```lua
function evaluate(request)
    for _, pattern in ipairs(config.patterns) do
        if string.find(request.body, pattern.regex) then
            return block("Sensitive content detected: " .. pattern.name)
        end
    end
    return allow()
end
```

**Recommendation**: Start with CEL for simple policies (it covers the current POC's needs), and add Lua support for complex policies that require procedural logic. Both can coexist: CEL for declarative rules, Lua for custom logic.

**This is a key area to explore further.** The policy distribution model (how policies are downloaded from the Gateway, versioned, and rolled out to the fleet) is an open question. See [Section 10 — Open Questions](#10-open-questions).

## 6. Gamma AI Fleet Module

The Gamma AI Fleet module is the control plane UI for monitoring and administering the DAImon fleet. It is a React-based module within the Gravitee Gamma console. See [diagram-gamma-fleet.md](diagram-gamma-fleet.md).

### 6.1. Current State (POC)

Three pages:

- **Fleet Page**: Lists registered DAImon devices with hostname, status (active/inactive based on heartbeat), and last-seen timestamp. Polls every 5 seconds.
- **Events Page**: Live view of intercepted traffic (model, token counts with system/history/user breakdown, latency) and direct connection detections. Shows both proxied requests and shadow AI connections.
- **Policies Page**: YAML editor with syntax highlighting (Prism.js) for editing the policy file. Changes are written to disk and picked up by DAImon's file watcher instantly.

### 6.2. Token Estimation

DAImon estimates token counts using a simple heuristic: 1 token ≈ 4 characters. It parses the full Anthropic request body to break down tokens by source:

- **System tokens**: The system prompt (injected by the AI tool, not the user)
- **History tokens**: All messages except the last one (conversation history sent on each request)
- **User tokens**: The final message (the user's current input)

This is an approximation, not real tokenization (which would require the provider's tokenizer). It is sufficient for budget enforcement and monitoring.

### 6.3. Shadow AI Observability

Shadow AI detection — identifying AI traffic that bypasses the Gateway — is a central value proposition, not a secondary feature. The Events page currently surfaces both proxied traffic and direct connection detections in a unified timeline.

For production, shadow AI observability should be expanded:

- **Dedicated warnings section**: Direct connection detections should be visually distinct from proxied traffic. When DAImon detects a tool connecting directly to an AI provider (bypassing the proxy), this should be surfaced as a warning with the process name, PID, target provider, and timestamp.
- **Aggregated shadow AI metrics**: Over time, the fleet console should show trends: how much AI traffic is managed (routed through DAImon) vs unmanaged (direct connections). This gives security teams a clear picture of their AI governance coverage.
- **Remediation suggestions**: When a shadow AI connection is detected, the console should suggest enforcement actions. For example: "Claude Code detected connecting directly to api.anthropic.com. To route this traffic through DAImon, set ANTHROPIC_BASE_URL=http://localhost:8990 in the tool's configuration, or deploy the corresponding MDM profile."

### 6.4. Future Features

The following features are not implemented but should be considered for the product roadmap:

- **Policy rollout and fleet update status**: Push new policies from the console, track which DAImon instances have applied the update, surface stale instances
- **Bypass alerts**: Real-time alerts when a DAImon detects direct connections bypassing the proxy (shadow AI usage)
- **Per-device and per-team policy assignment**: Different teams may have different policy sets (e.g., stricter DLP for teams handling customer data)
- **Cost dashboards**: Aggregate token usage and estimated cost across the fleet, broken down by team/user/model
- **Audit trail**: Full history of policy changes, who changed what, when

## 7. AI Gateway Endpoints

The AI Gateway exposes REST endpoints for DAImon communication. See [diagram-registration-heartbeat.md](diagram-registration-heartbeat.md) for the registration and heartbeat flow.

| Endpoint | Method | Purpose |
|---|---|---|
| `/daimon/register` | POST | Register a new DAImon instance (deviceId, hostname, user, version, OS, capabilities) |
| `/daimon/heartbeat` | POST | Periodic heartbeat with uptime and aggregated stats (requests, blocked, tokens) |
| `/daimon/config` | GET | Fetch the policy configuration assigned to this device |
| `/daimon/metrics` | POST | Push detailed metric events (per-request data) |
| `/daimon/devices` | GET | List all registered devices (used by Gamma UI) |

**POC vs Production storage**: In the POC, device state is stored in memory (`ConcurrentHashMap`) and metrics are written to local JSONL files on the Gateway. In production, device state and metrics should be stored in a persistent backend (Elasticsearch or database), enabling fleet-wide querying, alerting, and historical analysis via the AI Fleet control plane.

## 8. Out-of-the-Box Accelerators

To minimize time-to-value for organizations adopting DAImon, the initial release should include ready-to-use accelerators:

### 8.1. Default AI Provider Detection

DAImon should ship with a preconfigured list of major AI provider endpoints:

- api.anthropic.com (Anthropic / Claude)
- api.openai.com (OpenAI / ChatGPT)
- generativelanguage.googleapis.com (Google Gemini)

These defaults cover the most common AI tools. Organizations can add custom endpoints (e.g., Azure OpenAI instances, internal model servers) via the config.yaml or the AI Fleet control plane.

### 8.2. MDM Profiles

For the first release, provide a ready-to-deploy MDM profile (Kandji, Jamf) that:

- Installs the DAImon binary on the user's machine
- Configures `ANTHROPIC_BASE_URL=http://localhost:8990` as a system-level environment variable
- Starts DAImon as a background service on login
- Points DAImon to the organization's AI Gateway URL

This profile should be documented with step-by-step instructions so that IT administrators can deploy DAImon across their fleet without engineering support.

### 8.3. Starter Policy Pack

Include a default `policies.yaml` covering common use cases:

- **Secret detection**: AWS keys, GitHub tokens, generic passwords/secrets
- **Token budget**: Default limit per request (configurable threshold)
- **Model allowlist**: Empty by default (allow all), with documentation on how to restrict

These defaults provide immediate protection with zero configuration while remaining easy to customize.

## 9. Known Limitations and Tool-Specific Constraints

### 9.1. Claude Code

The POC focused exclusively on Claude Code. Key findings:

- **Partial routing**: Setting `ANTHROPIC_BASE_URL` routes the main conversation traffic through DAImon, but Claude Code also makes direct calls to `api.anthropic.com` for internal operations (telemetry, model selection, Haiku calls for lightweight tasks). These are not captured by the base URL override.
- **Multi-model usage**: Claude Code uses models other than the user-selected one. For example, it uses Haiku for classification and routing tasks. These internal calls may use different endpoints or headers.
- **No certificate pinning**: Claude Code does not pin certificates, which means the explicit proxy approach works without issues.

### 9.2. Other Tools (Not Yet Analyzed)

Each AI tool has its own architecture and constraints. A dedicated analysis phase is needed before supporting additional tools:

- **Cursor**: Known to implement certificate pinning, which would reject MITM-style interception. The explicit proxy approach (base URL override) would need to be evaluated for Cursor's specific configuration options.
- **GitHub Copilot**: Uses a VS Code extension with its own HTTP client. Base URL override mechanisms differ.
- **Gemini CLI**: Google's CLI tool. Configuration options for proxy routing need to be evaluated.
- **Windsurf, Cody, Continue, etc.**: Each has its own proxy/configuration model.

**This analysis phase should be prioritized** before committing to a multi-tool architecture. The goal is to document, for each tool:

1. Whether a base URL override is supported
2. Whether certificate pinning is implemented
3. Which internal calls bypass the main API endpoint
4. What telemetry or background requests are made

## 10. Open Questions

The following topics were identified during the POC but not fully explored. They require dedicated investigation before production:

### 10.1. TLS and Certificate Management

How should DAImon authenticate to the AI Gateway? mTLS with MDM-provisioned client certificates is the leading option, but the implementation details need to be defined:

- Certificate provisioning workflow (MDM → DAImon)
- Certificate rotation and revocation
- Gateway-side verification configuration
- Fallback behavior when certificates expire

### 10.2. Policy Distribution Model

How should policies be distributed from the Gateway to the fleet?

- Pull model (DAImon fetches on startup + periodically) vs push model (Gateway pushes updates)
- Policy versioning and rollback
- CEL/Lua script distribution: security implications of downloading and executing code
- Sandboxing guarantees for Lua scripts

### 10.3. Runtime-Loadable Policies (CEL / Lua)

The current policy engine compiles policies into the Go binary. Moving to CEL and/or Lua requires:

- Defining the policy API surface (what data is exposed to scripts)
- Sandboxing and resource limits (prevent infinite loops, memory exhaustion)
- Testing and validation workflow (how do policy authors test before deploying)
- Performance characteristics under load

### 10.4. Windows Support

The direct connection detector (shadow AI scanner) uses `lsof` on macOS/Linux (see Section 4.3). Windows support requires:

- Implementing the scanner behind the same abstraction using `netstat -ano`, `Get-NetTCPConnection`, or native Windows APIs
- Testing cross-compiled Go binaries on Windows
- MDM integration for Windows (Intune, etc.)

### 10.5. Multi-Tool Support

Each AI tool has different proxy configuration mechanisms and constraints. A systematic analysis is needed (see Section 9.2).

### 10.6. LLM Proxy Format Compatibility

The Gamma LLM Proxy currently accepts only OpenAI-compatible input format. DAImon forwards raw provider-native requests (e.g., Anthropic format from Claude Code). This creates a format mismatch.

Two approaches are possible:

- **DAImon translates**: DAImon converts provider-native requests to OpenAI format before forwarding to the LLM Proxy. This keeps the Gateway simple but adds complexity to DAImon (it must understand every provider's format).
- **Gateway extends**: The LLM Proxy adds support for Anthropic-native (and other provider-native) input formats. This keeps DAImon simple (pass-through proxy) but requires Gateway changes.

This is a key architectural decision that should be made in collaboration with the LLM Proxy team. The choice affects DAImon's complexity, the Gateway's API surface, and the overall integration model.

### 10.7. Group-Based Configuration

In a production fleet, different teams may require different policies and configurations. The architecture for group-based assignment (per-team policies, per-group provider lists, per-group model restrictions) needs to be defined:

- How are groups defined? (LDAP/AD integration, manual assignment, MDM-based)
- How does DAImon know which group it belongs to?
- How does the Gateway serve group-specific configurations?

### 10.8. Tool Identification

DAImon must infer which AI tool is making each request, even when multiple tools run on the same machine. This is needed for per-tool analytics and per-tool policy enforcement. Potential identification signals:

- **User-Agent header**: Some tools include identifying headers, but this is not standardized
- **Process detection**: Correlate the source port of the HTTP connection to a local PID/process name
- **Request shape heuristics**: Different tools have different request patterns (system prompts, message structures)

The reliability and privacy implications of each approach need to be evaluated. Process detection is the most reliable but may require elevated permissions on some platforms.

### 10.9. Shadow AI Classification

Shadow AI detection generates noise when it reports expected tool behavior as suspicious. For example, Claude Code's internal calls to api.anthropic.com are not bypasses — they are expected. The system needs a classification mechanism:

- How do administrators distinguish "expected direct connections" from "suspicious shadow AI"?
- Should the system ship with pre-built classification rules for known tools?
- How does the classification evolve as tools update their behavior?
- What metrics and alerting thresholds are meaningful when a portion of detected connections are expected?

# Gravitee Catalog MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server for **agent-centric discovery** of Gravitee assets: MCP proxies, LLM proxies, REST APIs, and async streams. It runs on **stdio** (intended for Cursor, Claude Desktop, or any MCP client that spawns a local process).

In this proof-of-concept, the “catalog” is a local `src/catalog.json` file. A future version would list assets from the Gamma control plane using the same environment variables you configure today.

## Prerequisites

- Node.js **^22.12** (same as the APIM monorepo)
- Yarn (this package uses **Yarn 4** with its own `yarn.lock` so it is installable standalone under `gravitee-gamma/packages/`)
- An **OpenAI API key** (semantic search uses `gpt-4o-mini` to match natural-language intents to catalog entries)
- For Cursor: the [`tsx`](https://github.com/privatenumber/tsx) runner on your `PATH` (the repo’s MCP config uses `npx tsx` from the monorepo root; after `yarn install` in this package, `npx` can also resolve the local `tsx` when run from this directory)

## Install

From this directory:

```bash
cd gravitee-gamma/packages/gravitee-catalog-mcp-server
yarn install
```

## Environment variables

| Variable | Required | Description |
| -------- | -------- | ----------- |
| `GRAVITEE_GAMMA_BASE_URL` | Yes | Base URL of the Gravitee Gamma control plane (used in subscription responses, e.g. for console links) |
| `GRAVITEE_APP_ID` | Yes | Application identifier (reserved for real integrations) |
| `GRAVITEE_API_KEY` | Yes | API key (reserved for real integrations) |
| `OPENAI_API_KEY` | Yes | OpenAI key for `search_gravitee_catalog` |

The server **exits immediately** with a clear error if any of these is missing.

### Example (local shell)

```bash
export GRAVITEE_GAMMA_BASE_URL="https://gamma.example.com"
export GRAVITEE_APP_ID="my-app"
export GRAVITEE_API_KEY="dev-placeholder"
export OPENAI_API_KEY="sk-..."
```

## Tools

| Tool | What it does |
| ---- | ------------ |
| `search_gravitee_catalog` | **Input:** `{ "intent": "<natural language>" }` — loads `src/catalog.json`, calls OpenAI to pick the top **2** best matches, returns JSON with `id`, `name`, `type`, `description`, `relevance_score`, `reason`. |
| `request_asset_subscription` | **Input:** `{ "asset_id": "<id>", "justification": "<text>" }` — validates the asset exists, returns a mock **202 Accepted** with a `request_id` and a message describing human-in-the-loop approval via Gravitee Access Management. |

## Use with Cursor

1. **Configure MCP** (monorepo root) — a starter file lives at [`.cursor/mcp.json`](../../../.cursor/mcp.json) in this repository. Point the command at this package’s `src/index.ts` and set the four environment variables to real values (not placeholders).
2. **Paths:** The sample config uses paths **relative to the monorepo root** (`gravitee-gamma/packages/gravitee-catalog-mcp-server/...`). Open the repo at that root in Cursor so `npx tsx` resolves the entry file.
3. **Reload** MCP: Command Palette → **MCP: List Servers** (or restart Cursor) so the `gravitee-catalog` server and its two tools appear.
4. In chat, ask the agent to use the tools, e.g. *“Use search_gravitee_catalog to find something for taking payments, then request access with a one-line business justification.”*

`yarn start` in this package also runs the server; it is meant to be **spawned by an MCP client** (stdio), not run interactively in a terminal for normal use.

## Example intents (`search_gravitee_catalog`)

Try these as the `intent` (or as natural phrasing in Cursor):

| You want the agent to find | Example `intent` |
| -------------------------- | ------------------ |
| Card charges / Stripe | *“I need to charge credit cards and manage subscriptions for my app.”* |
| Slack | *“Send messages and alerts to our team Slack from automation.”* |
| GitHub | *“Create and triage issues and link them to our repos.”* |
| Salesforce | *“Sync contact and account data with our CRM.”* |
| On-prem LLM | *“A private model for code review and internal documents, on our infrastructure.”* |
| Enterprise GPT-4o | *“A general OpenAI model with org controls and usage limits.”* |
| Customer PII / GDPR | *“Read and update customer profiles with GDPR export.”* |
| Stock / warehouse | *“Check inventory levels and reserve items before shipping.”* |
| Kafka / order events | *“Subscribe to order lifecycle events for our fulfillment pipeline.”* |

## Example subscription (`request_asset_subscription`)

Use an `asset_id` from the catalog. Valid IDs in the mock file include:

- `salesforce-crm-mcp`
- `stripe-payments-mcp`
- `github-issues-mcp`
- `slack-notifications-mcp`
- `internal-llama3-proxy`
- `gpt4o-enterprise-proxy`
- `customer-data-api`
- `inventory-service-api`
- `order-events-stream`

**Example body:**

```json
{
  "asset_id": "stripe-payments-mcp",
  "justification": "Building a checkout flow for the Q2 hackathon demo; need test-mode access to create PaymentIntents only."
}
```

A successful response is shaped like:

- `status`: `"202 Accepted"`
- `request_id`: a UUID
- `message`: explains pending approval and owner review
- `gravitee_console_url`: URL built from `GRAVITEE_GAMMA_BASE_URL` (demo link only in the POC)

If the ID is wrong, the tool returns a `404` style error JSON listing valid usage.

## Project layout

```
src/
  index.ts              # MCP server + tool registration
  config.ts             # env validation
  catalog.json          # mock assets
  tools/
    search-catalog.ts
    request-subscription.ts
```

## License

Apache-2.0, consistent with the Gravitee API Management monorepo.

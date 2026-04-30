# Gravitee Catalog MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server for **agent-centric discovery** of Gravitee assets: MCP proxies, LLM proxies, REST APIs, and async streams. It runs on **stdio** (intended for Cursor, Claude Desktop, or any MCP client that spawns a local process).

Search queries are forwarded to the **Portal API** `/catalog/search` endpoint, which performs hybrid semantic + keyword search over the indexed API catalog.

## Prerequisites

- Node.js **^22.12** (same as the APIM monorepo)
- Yarn (this package uses **Yarn 4** with its own `yarn.lock` so it is installable standalone under `gravitee-gamma/packages/`)
- A running **Gravitee APIM Portal API** with the catalog search endpoint enabled
- For Cursor: the [`tsx`](https://github.com/privatenumber/tsx) runner on your `PATH`

## Install

From this directory:

```bash
cd gravitee-gamma/packages/gravitee-catalog-mcp-server
yarn install
```

## Environment variables

| Variable | Required | Description |
| -------- | -------- | ----------- |
| `PORTAL_API_BASE_URL` | Yes | Base URL of the Gravitee Portal API (e.g. `http://localhost:8083/portal/environments/DEFAULT`) |
| `PORTAL_API_USERNAME` | Yes | Username for Portal API basic authentication |
| `PORTAL_API_PASSWORD` | Yes | Password for Portal API basic authentication |

All variables are **required**. The process **exits** with a clear error if any is missing.

### Example (local shell)

```bash
export PORTAL_API_BASE_URL="http://localhost:8083/portal/environments/DEFAULT"
export PORTAL_API_USERNAME="admin"
export PORTAL_API_PASSWORD="admin"
```

## Tools

| Tool | What it does |
| ---- | ------------ |
| `search_gravitee_catalog` | **Input:** `{ "intent": "<natural language>" }` — calls the Portal API `/catalog/search` endpoint with hybrid semantic search. Returns matching assets with `id`, `title`, `description`, `type`, `owner`, `tags`, `score`, and more. |

## Use with Cursor

1. **Configure MCP** (monorepo root) — a starter file lives at [`.cursor/mcp.json`](../../../.cursor/mcp.json) in this repository. It can load env from a **gitignored** [`.env.mcp`](../../../.env.mcp) at the repo root; do not commit secrets.
2. **Paths:** The sample config uses paths **relative to the monorepo root** (`gravitee-gamma/packages/gravitee-catalog-mcp-server/...`). Open the repo at that root in Cursor so `npx tsx` resolves the entry file.
3. **Reload** MCP: Command Palette → **MCP: List Servers** (or restart Cursor) so the `gravitee-catalog` server and its tool appear.
4. In chat, ask the agent to use the tool, e.g. *"Use search_gravitee_catalog to find something for taking payments."*

`yarn start` in this package also runs the server; it is meant to be **spawned by an MCP client** (stdio), not run interactively in a terminal for normal use.

## Example intents (`search_gravitee_catalog`)

Try these as the `intent` (or as natural phrasing in Cursor):

| You want the agent to find | Example `intent` |
| -------------------------- | ------------------ |
| Card charges / Stripe | *"I need to charge credit cards and manage subscriptions for my app."* |
| Slack | *"Send messages and alerts to our team Slack from automation."* |
| GitHub | *"Create and triage issues and link them to our repos."* |
| Salesforce | *"Sync contact and account data with our CRM."* |
| On-prem LLM | *"A private model for code review and internal documents, on our infrastructure."* |
| Enterprise GPT-4o | *"A general OpenAI model with org controls and usage limits."* |

## Project layout

```
src/
  index.ts              # MCP server + tool registration
  config.ts             # env validation
  tools/
    search-catalog.ts   # Portal API search proxy
```

## License

Apache-2.0, consistent with the Gravitee API Management monorepo.

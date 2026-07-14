# AI Product POC — Solution Engineer quick start

Run the July AI Product demo locally in ~5 minutes.

## What you need

1. **Docker Desktop** (or OrbStack)
2. **ACR access** — `docker login graviteeio.azurecr.io`
3. **EE license** — decoded binary file (ask your Gravitee contact)
4. **This tar** — `ai-product-poc.tar` (or clone `poc-ai-products` branch)

## Start the stack

```bash
tar -xf ai-product-poc.tar && cd stack

# License (decoded binary — NOT the base64 text file)
base64 --decode < /path/to/license.base64.txt > license/license.key

# Optional: license via env instead of file mount
cp .env.example .env
# edit LICENSE_KEY=<base64-string>

docker login graviteeio.azurecr.io
docker compose up -d
```

Wait ~2 minutes. Open **http://localhost:8085** → log in `admin` / `admin`.

Navigate to **AI Products** in the Gamma sidebar.

## Seed demo data (optional)

Clone the APIM repo (`poc-ai-products` branch) for scripts:

```bash
git clone -b poc-ai-products <apim-repo-url>
cd gravitee-api-management

python3 ai-poc-demo/mock-llm.py 9099 &

LLM_API_KEY=unused LLM_TARGET=http://host.docker.internal:9099/v1 \
  LLM_MODEL=gpt-4o-mini ai-poc-demo/seed.sh

API_ID=<id-from-seed-output> ai-poc-demo/verify.sh
```

On Linux, use `172.17.0.1` instead of `host.docker.internal`.

## Demo flow (July video)

1. **Catalog** — Home → **Add Integration** (or Catalog → AI Models → Import) to add an LLM model
2. **LLM Proxy** — create a proxy API wired to that model (or use seeded proxy from `seed.sh`)
3. **AI Product** — create product, attach the LLM component, create an **AUTO** plan with token budget
4. **Users** — open the product → **Users** tab → **Add user** (email + per-user token budget); copy the API key
5. **Gateway** — `curl` the product endpoint with `X-Gravitee-Api-Key` → **429** when budget is exhausted

```bash
# Example (replace host, key, and path from the product overview)
curl -sS -X POST "http://localhost:8082/ai-products/<product-path>/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -H "X-Gravitee-Api-Key: <api-key>" \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}'
```

## Services

| URL | Purpose |
|-----|---------|
| http://localhost:8085 | Gamma console (AI Products UI) |
| http://localhost:8083 | Management API |
| http://localhost:8082 | Gateway |
| http://localhost:2580 | Fake SMTP inbox |

## Plugin update (advanced)

Drop a new AIM plugin zip into `stack/plugins/` and restart:

```bash
docker compose restart management-api
```

## Help

See `DOCKER_ACR_RUNBOOK.md` in the APIM repo for build/push instructions (maintainers).

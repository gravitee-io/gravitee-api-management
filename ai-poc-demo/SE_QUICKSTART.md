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

1. Admin creates an **AI Product** in Gamma console
2. Adds an **LLM component** (from seeded proxy API)
3. Creates a **plan** with per-user token budget
4. Adds **developers** with individual limits
5. Developer calls the gateway → **429** when budget exhausted

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

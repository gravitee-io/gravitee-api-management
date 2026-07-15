# AI Product POC — Docker build & ACR push

> **One command:** `./build-and-push.sh --push`  
> **SE runtime:** `stack/` folder (gamma-dev style)

## What gets built

| Artifact | Source | Registry image |
|----------|--------|----------------|
| Gateway + LLM plugins | `poc-ai-products` APIM branch | `graviteeio.azurecr.io/apim-gateway:<tag>` |
| Management API + AIM UI | AIM `poc/ai-products` → APIM distribution | `graviteeio.azurecr.io/apim-management-api:<tag>` |
| Gamma console host | Pre-built `gamma-ui:4.12.x-latest` | (no rebuild needed for POC) |

Default tag: `ai-product-poc`

## Prerequisites

```bash
java -version          # JDK 21
mvn -version           # Maven 3.9+
node -v                # 22.x
corepack enable
docker --version
docker login graviteeio.azurecr.io
```

Repos (branches):

- `gravitee-api-management` → `poc-ai-products`
- `gravitee-gamma-module-aim` → `poc/ai-products`
- `gravitee-reactor-llm-proxy` → ApiProduct wiring

## Build & push (maintainer)

```bash
cd ai-poc-demo
./build-and-push.sh --push
```

Optional: `./build-and-push.sh --pack` → `ai-product-poc.tar`

## Teammate: fresh pull & run

```bash
cd ai-poc-demo/stack

# 1. Stop stack and remove stale local images
docker compose down
docker rmi graviteeio.azurecr.io/apim-management-api:ai-product-poc \
           graviteeio.azurecr.io/apim-gateway:ai-product-poc 2>/dev/null || true

# 2. Pull latest from ACR
docker login graviteeio.azurecr.io
docker compose pull
docker compose up -d

# 3. Seed catalog (from APIM repo root)
cd ../..
./ai-poc-demo/setup-demo-models.sh

# 4. Console demo or automated test
open http://localhost:8085   # admin / admin → AI Products → Create
./ai-poc-demo/test-ai-product.sh
```

## Console-first demo flow

1. `./setup-demo-models.sh` — catalog models, mock upstream, no provider keys
2. **AI Products → Create** — models + token budget (day/week/month) → **Create & deploy**
3. **Users** tab — add user + budget → copy API key
4. `curl` gateway entrypoint until **429**

No provider API key in Create AI Product — upstream auth follows catalog source (`NONE` for mock demo).

## Endpoints

| Service | URL |
|---------|-----|
| Gamma console (AI Products) | http://localhost:8085 |
| Management API | http://localhost:8083 |
| Gateway | http://localhost:8082 |
| Mock LLM | http://localhost:9099 |
| Fake SMTP | http://localhost:2580 |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Old UI (Provider API key field) | `docker compose pull` + hard refresh (`Cmd+Shift+R`) |
| Catalog import 500 | Pull latest management-api (AIM plugin bundles gamma-definition-model) |
| `token-ratelimit` missing | Pull latest gateway image |
| Create fails on path | Entrypoint already taken — pick a different path |
| 502 from gateway | Ensure `mock-llm` service is healthy in compose |

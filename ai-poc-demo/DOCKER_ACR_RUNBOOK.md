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
# Toolchain
java -version          # JDK 21
mvn -version           # Maven 3.9+
node -v                # 22.x
corepack enable
docker --version

# Registry
docker login graviteeio.azurecr.io

# Repos (branches)
gravitee-api-management     → poc-ai-products
gravitee-gamma-module-aim   → poc/ai-products
gravitee-reactor-llm-proxy  → (ApiProduct wiring)
```

You also need **internal Maven** access for EE plugins (`token-ratelimit`, etc.) and a **Gravitee EE license** for the 429 token-budget demo.

## Build & push (maintainer)

```bash
cd ai-poc-demo

# Optional overrides
cp build.conf.example build.conf

# Full pipeline: AIM → LLM proxy → APIM → Docker → ACR
./build-and-push.sh --push

# Create SE distributable tar
./build-and-push.sh --pack
# → ai-product-poc.tar (stack/ folder only)
```

### Partial runs

```bash
SKIP_AIM=1 ./build-and-push.sh          # APIM already has AIM in ~/.m2
./build-and-push.sh --docker-only       # images from existing target/ distributions
SKIP_LLM_PROXY=1 ./build-and-push.sh    # use Maven-cached LLM plugins
```

### Manual equivalent (Taskfile)

```bash
# After mvn install on poc-ai-products with AIM installed locally:
DOCKER_TAG=ai-product-poc DOCKER_REGISTRY=graviteeio.azurecr.io task docker-backend
docker push graviteeio.azurecr.io/apim-gateway:ai-product-poc
docker push graviteeio.azurecr.io/apim-management-api:ai-product-poc
```

## Teammate: run the POC

```bash
# Option A — self-contained stack (recommended)
tar -xf ai-product-poc.tar && cd stack
base64 --decode < license.base64.txt > license/license.key
docker login graviteeio.azurecr.io
docker compose up -d
# → http://localhost:8085  (admin / admin)

# Option B — from APIM repo checkout
cd ai-poc-demo/stack
export POC_IMAGE_TAG=ai-product-poc
docker compose up -d
```

Seed demo (from APIM repo root):

```bash
python3 ai-poc-demo/mock-llm.py 9099 &
LLM_API_KEY=unused LLM_TARGET=http://host.docker.internal:9099/v1 \
  LLM_MODEL=gpt-4o-mini ai-poc-demo/seed.sh
API_ID=<from seed> ai-poc-demo/verify.sh
```

## Endpoints

| Service | URL |
|---------|-----|
| Gamma console (AI Products) | http://localhost:8085 |
| Management API | http://localhost:8083 |
| Gateway | http://localhost:8082 |
| Fake SMTP | http://localhost:2580 |

## Architecture note

- **Admin UI** = `gamma-ui` + AIM plugin (AI Products screens live in `gravitee-gamma-module-aim`, not gamma-module-apim)
- **Developer portal** = not in Docker yet; portal-next runs via `yarn nx serve portal-next` for full demo
- **Plugin hot-reload** = drop AIM zip in `stack/plugins/`, `docker compose restart management-api`

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `token-ratelimit` missing | Set `LICENSE_KEY` or mount `license/license.key` |
| Gamma 404 / no AIM | Confirm `gravitee_gamma_enabled=true`; AIM zip in image or `./plugins` |
| Port 27017/9200 busy | Stop other APIM docker stacks |
| MapStruct compile errors | Always `mvn clean install`, never incremental-only |
| AIM UI build fails | Run `yarn build` in AIM repo; check `poc/ai-products` branch |

# AI Product POC stack

Self-contained **gamma-dev-style** Docker stack for the July AI Product demo.
Ship this folder (or a `.tar` of it) to Solution Engineers.

## Layout

```
stack/
  compose.yaml       # full stack (mongo, es, redis, gateway, management-api, gamma-ui)
  plugins/           # gateway plugin zips (LLM proxy, token-ratelimit); AIM is in the management-api image
  license/           # decoded EE license3j binary → license.key
  mise.toml          # mise run up | down | restart | seed
  README.md
```

Sibling scripts (parent `ai-poc-demo/` folder, from `poc-ai-products` branch checkout):

```
../seed.sh           # create LLM proxy API
../verify.sh         # end-to-end AI Product + 429 check
../mock-llm.py       # offline OpenAI-compatible upstream
```

## Prerequisites

- Docker Desktop (or OrbStack)
- `docker login graviteeio.azurecr.io` (Gravitee ACR credentials)
- Gravitee **EE license** — embedded in `compose.yaml` (Internal Development & Testing key)
- For seed/verify: clone `gravitee-api-management` branch `poc-ai-products`

## Quick start (SE)

```bash
# 1. Unpack (if you received ai-product-poc.tar)
tar -xf ai-product-poc.tar && cd ai-product-poc/stack

# 2. License — embedded in compose.yaml (gamma-dev style). No file needed for local dev.
#    To override: edit x-license &key in compose.yaml

# 3. Login + start
docker login graviteeio.azurecr.io
docker compose up -d

# 4. Wait ~2 min, check plugins
docker compose logs gateway 2>&1 | grep -iE "token-ratelimit|llm" | head
docker compose logs management-api 2>&1 | grep -i gamma | head
```

Open **http://localhost:8085** (Gamma console) — log in `admin` / `admin`.

## Seed demo data

From the APIM repo root (`poc-ai-products` branch):

```bash
python3 ai-poc-demo/mock-llm.py 9099 &

LLM_API_KEY=unused LLM_TARGET=http://host.docker.internal:9099/v1 \
  LLM_MODEL=gpt-4o-mini ai-poc-demo/seed.sh

API_ID=<from seed> ai-poc-demo/verify.sh
```

On **Linux**, replace `host.docker.internal` with `172.17.0.1` or your host IP.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Blank page at `/aim/import/models` | Pull latest `apim-management-api:ai-product-poc` (includes AIM route fix) |
| Catalog → AI Models returns HTTP 500 | Pull latest management-api image (includes `ENVIRONMENT_AI_CATALOG` permission fix) |
| `Plugin [llm-proxy] cannot be found` on seed/API create | Pull latest gateway image (includes LLM proxy plugins) or run `build-and-push.sh` and recreate gateway |
| Only APIM + Platform visible, no AIM | AIM `feature=gamma-aim-module` needs that name in the license **features** list (not just the `agent-management` pack). POC plugin omits the feature gate; pull latest management-api image |

```bash
cp gravitee-gamma-module-aim/target/gravitee-gamma-module-aim-*.zip plugins/
docker compose restart management-api
```

## Pack for distribution

```bash
./pack.sh
# or: ./build-and-push.sh --pack
```

## Build new images (maintainers)

```bash
cd .. && ./build-and-push.sh --push
```

Default image tag: `ai-product-poc` (override with `POC_IMAGE_TAG`).


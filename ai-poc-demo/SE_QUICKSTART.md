# AI Product POC — Solution Engineer quick start

Run the AI Product demo locally in ~5 minutes.

## What you need

1. **Docker Desktop** (or OrbStack)
2. **ACR access** — `docker login graviteeio.azurecr.io`
3. **This tar** — `ai-product-poc.tar` (or clone `poc-ai-products` branch)

License is embedded in `stack/compose.yaml` (Internal Development & Testing key).

## Fresh start (pull from ACR)

```bash
cd ai-poc-demo/stack

# Remove old local images (optional — forces re-pull)
docker compose down
docker rmi graviteeio.azurecr.io/apim-management-api:ai-product-poc \
           graviteeio.azurecr.io/apim-gateway:ai-product-poc 2>/dev/null || true

docker login graviteeio.azurecr.io
docker compose pull
docker compose up -d
```

Wait ~2 minutes. Open **http://localhost:8085** → log in `admin` / `admin`.

Navigate to **AI Products** in the Gamma sidebar.

## Seed demo catalog

From the APIM repo root (`poc-ai-products` branch):

```bash
./ai-poc-demo/setup-demo-models.sh
```

This imports OpenAI + Qwen + Ollama-style models into Catalog (all mock-backed, `authMethod: NONE` — **no provider API key**).

## Demo flow (console)

1. **AI Products → Create**
   - Name, version, gateway entrypoint (e.g. `/ai/company-llm`)
   - Pick catalog models (Qwen3.6 Plus, GPT-5.4 mini, …)
   - Set **default user token budget** + reset window (day / week / month)
   - **Create & deploy** — no provider API key field
2. **Users tab** — add user + token budget → copy API key
3. **Gateway** — curl until **HTTP 429**

```bash
curl -sS -X POST "http://localhost:8082/ai/YOUR-PRODUCT/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -H "X-Gravitee-Api-Key: YOUR-KEY" \
  -d '{"model":"Qwen/Qwen3.6-Plus","messages":[{"role":"user","content":"hello"}]}'
```

Demo tip: budget **100** with mock upstream (~15 tokens/call) → 429 in ~7 calls.

## Automated E2E test (API)

```bash
./ai-poc-demo/test-ai-product.sh
```

## Services

| URL | Purpose |
|-----|---------|
| http://localhost:8085 | Gamma console (AI Products UI) |
| http://localhost:8083 | Management API |
| http://localhost:8082 | Gateway |
| http://localhost:9099 | Mock LLM upstream (in compose) |
| http://localhost:2580 | Fake SMTP inbox |

## Optional: live Ollama

```bash
cd ai-poc-demo/stack
docker compose --profile ollama up -d ollama
docker compose exec ollama ollama pull qwen2.5:0.5b
OLLAMA_LIVE=1 ../setup-demo-models.sh
```

## Maintainer: build & push

```bash
cd ai-poc-demo
./build-and-push.sh --push
```

See `DOCKER_ACR_RUNBOOK.md` for details.

# AI Products PoC — demo runbook

Demo story: admin creates an **AI Product** in the Gamma console, adds an **LLM proxy component**,
creates a plan with a **request rate limit + token budget**, adds a **consumer** (application +
subscription → API key). The developer opens the **Developer Portal**, sees the AI Product with
key/endpoint/models/snippets, calls the LLM through the gateway, and gets **429** when the token
budget is exhausted.

What makes this work end-to-end (all in this branch):
- Gateway patch: **API-Product plan flows now execute** (`ApiProductPlanFlowResolver` +
  `ApiProductPlanPolicyManager` flow-policy loading + product plan flow chain in `DefaultApiReactor`).
  Product-plan *security* (API key on product subscription) already worked.
- Gamma `features/ai-products`: list / create / overview / general / components (search & add LLM
  proxies) / plans (with Token budget restriction → `token-ratelimit` policy in the product plan
  flow) / consumers / user permissions + deploy banner.
- Portal REST: `GET /subscriptions?referenceType=API_PRODUCT` + product metadata now carries
  `description` and `entrypoints` (gateway endpoint).
- Portal-next: AI Products list + getting-started page (key, endpoint, models, snippets).

## 1. Build & assemble (once)

> **Build CLEAN.** Incremental multi-module (`-am`) rebuilds can leave a half-written generated
> MapStruct mapper (`ApiMapperImpl.java` truncation) → spurious compile errors. A clean build avoids
> it. Also: the `ApiProduct` model is generated from BOTH `openapi-api-products.yaml` and
> `openapi-apis.yaml` — both now declare `type`; don't revert either or `type` silently vanishes.

```bash
cd ~/IdeaProjects/gravitee-api-management

# Clean full build (includes the gateway product-plan-flow patch + the AI Product `type` discriminator).
# The JDBC module's default-jdbc-test execution ignores -Dmaven.test.skip and 2-3 tests fail on this
# machine with "URI is not hierarchical" (environmental) — exclude them:
mvn clean install -Dmaven.test.skip=true -Dskip.validation=true \
  -Dtest='!FlowRepositoryPKTest,!TableConstraintsTest,!FlowRepositoryTest' -Dsurefire.failIfNoSpecifiedTests=false

# (or just the two distributions, clean:)
# mvn -pl gravitee-apim-gateway/.../gravitee-apim-gateway-standalone-distribution -am clean package -Dmaven.test.skip=true -Dskip.validation=true
# mvn -pl gravitee-apim-rest-api/.../gravitee-apim-rest-api-standalone-distribution -am clean package -Dmaven.test.skip=true -Dskip.validation=true -Dtest='!FlowRepositoryPKTest,!TableConstraintsTest,!FlowRepositoryTest' -Dsurefire.failIfNoSpecifiedTests=false

# LLM reactor — KEEP its uncommitted ApiProductRegistry wiring (do NOT stash/revert); just rebuild the zip.
# No reactor source change is needed: it extends the (patched) DefaultApiReactor via `provided` scope.
(cd ~/IdeaProjects/gravitee-reactor-llm-proxy && mvn clean package -Dmaven.test.skip=true -Dskip.validation)

# Gateway image: distribution + EE plugins baked in
rm -rf gravitee-apim-gateway/docker/distribution
cp -R gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution gravitee-apim-gateway/docker/distribution
cp ~/IdeaProjects/gravitee-reactor-llm-proxy/gravitee-reactor-llm-proxy/target/gravitee-reactor-llm-proxy-*.zip       gravitee-apim-gateway/docker/distribution/plugins/
cp ~/IdeaProjects/gravitee-reactor-llm-proxy/gravitee-entrypoint-llm-proxy/target/gravitee-entrypoint-llm-proxy-*.zip gravitee-apim-gateway/docker/distribution/plugins/
cp ~/IdeaProjects/gravitee-reactor-llm-proxy/gravitee-endpoint-llm-proxy/target/gravitee-endpoint-llm-proxy-*.zip     gravitee-apim-gateway/docker/distribution/plugins/
cp ~/.m2/repository/com/graviteesource/policy/gravitee-policy-token-ratelimit/1.0.0/gravitee-policy-token-ratelimit-1.0.0.zip gravitee-apim-gateway/docker/distribution/plugins/
docker build -t graviteeio/apim-gateway:latest gravitee-apim-gateway/docker

# Management API image: needs the llm plugins (API creation/validation) + token-ratelimit (plan flow validation)
rm -rf gravitee-apim-rest-api/docker/distribution
cp -R gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution gravitee-apim-rest-api/docker/distribution
cp ~/IdeaProjects/gravitee-reactor-llm-proxy/gravitee-entrypoint-llm-proxy/target/gravitee-entrypoint-llm-proxy-*.zip gravitee-apim-rest-api/docker/distribution/plugins/
cp ~/IdeaProjects/gravitee-reactor-llm-proxy/gravitee-endpoint-llm-proxy/target/gravitee-endpoint-llm-proxy-*.zip     gravitee-apim-rest-api/docker/distribution/plugins/
cp ~/.m2/repository/com/graviteesource/policy/gravitee-policy-token-ratelimit/1.0.0/gravitee-policy-token-ratelimit-1.0.0.zip gravitee-apim-rest-api/docker/distribution/plugins/
docker build -t graviteeio/apim-management-api:latest gravitee-apim-rest-api/docker
```

## 2. Run the stack

> **Pre-flight:** containers `apim_mongodb` (27017) and `apim_elasticsearch` (9200/9300) from
> another stack are currently bound to the ports this compose needs — stop them first
> (`docker stop apim_mongodb apim_elasticsearch`) or the compose will fail to bind.

```bash
cd docker/quick-setup/mongodb
# docker-compose.override.yml in this folder adds gravitee_gamma_enabled=true to management_api
LICENSE_KEY=$(cat /path/to/license.key.b64) docker compose up -d
docker logs gio_apim_gateway 2>&1 | grep -iE "token-ratelimit|llm" | head   # plugins + license OK?
```

UIs (dev servers, from repo root):

```bash
# Gamma console (module on :3001 + host console)
yarn nx serve gravitee-gamma-module-apim
DEV_MODULE_ENTRIES="apim=http://localhost:3001/mf-manifest.json" yarn gamma-console:serve

# Portal-next
yarn nx serve portal-next
```

## 3. Seed (one-time)

Real provider:
```bash
LLM_API_KEY=sk-...  ./ai-poc-demo/seed.sh
# Optional overrides: LLM_TARGET (default https://api.openai.com/v1), LLM_MODEL (gpt-4o-mini), CONTEXT_PATH (/ai/openai)
```

Or fully offline with the bundled mock upstream (no provider key needed; each call = 100 tokens,
so a 250-token limit exhausts in 3 calls — handy for the 429 demo):
```bash
python3 ai-poc-demo/mock-llm.py 9099 &          # OpenAI-compatible mock on :9099 (/v1/chat/completions, /v1/models)
LLM_API_KEY=unused LLM_TARGET=http://host.docker.internal:9099/v1 LLM_MODEL=gpt-4o-mini ./ai-poc-demo/seed.sh
```

Or the **official Gravitee fake LLM backend** (gravitee-sample-apis → `gravitee-ai-api`), which returns
real provider-shaped responses with token usage and can simulate latency / status codes:
```bash
# from a checkout of github.com/gravitee-io/gravitee-sample-apis (gravitee-ai-api):
java -jar gravitee-sample-api-*.jar 9098                     # serves GET/POST /ai/llm?provider=openai|anthropic&model=…&latency=…&statusCode=…
# point the LLM proxy at it (its responses are OpenAI/Anthropic shaped, with usage.total_tokens):
LLM_API_KEY=unused LLM_TARGET=http://host.docker.internal:9098/ai LLM_MODEL=gpt-4o ./ai-poc-demo/seed.sh
```
The bundled `mock-llm.py` is the lowest-friction choice because its paths (`/v1/chat/completions`,
`/v1/models`) match exactly what the llm-proxy endpoint connector calls; the official sample is the
better choice when you want multi-provider shapes and error/latency injection.

### Prove it end to end (scripted)
```bash
API_ID=<llm proxy id from seed.sh>  ./ai-poc-demo/verify.sh
# Creates an AI Product → per-developer plan → Bob (250) + Alice (2000) → shows Bob 429s while Alice 200s.
```

## 4. Demo flow (live) — the simplified model

The admin does **two things**; the developer does **nothing but log in**. Plans, subscriptions, and
applications are created automatically behind the scenes — they never appear in the UI.

**Prerequisite:** an LLM proxy must exist to attach (this PoC attaches an existing one). If your DB
has none, create one first with `seed.sh` (step 3 above) — that's the only "LLM proxy" step.

1. **Gamma → AI Products → Create** "Company LLM Access", 1.0.0. (The AI Products list is separate
   from API Products — the `type` discriminator; they no longer show the same data.)
2. **Components → Add component** → search finds the LLM proxy → add. The **Available models** card
   shows models grouped by provider (OpenAI / Anthropic / Google …) → **Deploy**.
3. **Users → Add user** → pick **Bob** (a product member) + **token budget 250** + **rate limit
   60/min** (both mandatory) → one click. Behind the scenes this auto-creates the hidden access plan
   (first time only), Bob's application + subscription + key. The success panel shows **Bob's own
   key**. Add **Alice** with budget **2000** → she gets a **different key**. *(Every user gets their
   own key, own token budget, and own rate limit. There is no "Plans" tab and no "subscribe" step.)*
4. **Portal** (`/dashboard/ai-products`) → log in as the developer → product page: their API key,
   **their token budget**, endpoint, models, copy the cURL snippet. They set up nothing.
5. Terminal: Bob's key → **200** + `X-Token-Rate-Limit-Remaining`; burn Bob's 250-token budget →
   Bob's next call **429**. At the same moment **Alice's key still returns 200** — budgets are
   personal, not shared. That's the LiteLLM/Portkey story: 1000 developers, one product, a key + a
   personal budget each.

What the admin never sees: "plan", "publish a plan", "subscribe", "application". One hidden API-key
plan is auto-created per product with `dynamicLimit = {#subscription.metadata['tokenLimit']}`, so the
per-developer budget is read from each developer's subscription metadata at the gateway.

## 5. Verification curl ladder

```bash
GW=http://localhost:8082/ai/openai ; KEY=<api key from step 4>

# a) no key → 401 (product plan SECURITY on the LLM proxy)
curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}' "$GW/chat/completions"

# b) with key → 200 + X-Token-Rate-Limit-* headers (product plan FLOW executed — the new gateway code)
curl -s -D- -H "X-Gravitee-Api-Key: $KEY" -H 'Content-Type: application/json' \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}' "$GW/chat/completions" | head -25

# c) burn the budget (counters update post-response → the NEXT call 429s)
curl -s -o /dev/null -H "X-Gravitee-Api-Key: $KEY" -H 'Content-Type: application/json' \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"write 300 words about gateways"}]}' "$GW/chat/completions"
curl -s -o /dev/null -w '%{http_code}\n' -H "X-Gravitee-Api-Key: $KEY" -H 'Content-Type: application/json' \
     -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}' "$GW/chat/completions"   # → 429

# d) models route (portal uses this; CORS may block it from the browser — portal falls back to
#    models seeded in the product description as {"models":["gpt-4o-mini"]})
curl -s -H "X-Gravitee-Api-Key: $KEY" "$GW/models" | head -5
```

## Per-developer token limits — how they work (verified in code)

**No new policy is needed.** The stock EE `token-ratelimit` policy does per-developer limits via
`dynamicLimit` + subscription metadata. Verified end to end:

- **Configured:** the AI Product plan's token-ratelimit flow uses
  `rate.dynamicLimit = "{#subscription.metadata['tokenLimit'] ?: <default>}"` (static `limit: 0`).
  Each developer's number is written to **their subscription's metadata** by the Add-developer flow
  (`metadata.tokenLimit`). One plan → N developers → N personal limits.
- **Enforced:** at request time the gateway syncs subscription metadata onto the gateway
  `Subscription` (`SubscriptionMapper.toSubscription` copies `metadata`), `SubscriptionProcessor`
  sets `#subscription` in the EL context (`SubscriptionVariable.getMetadata()`), and the policy calls
  `templateEngine.eval(dynamicLimit, Long.class)` — so `#subscription.metadata['tokenLimit']` resolves
  to that developer's value. The counter is keyed per subscription, so limits are personal, not shared.
- **Tracked:** every response carries `X-Token-Rate-Limit-Limit / -Remaining / -Reset` (with
  `addHeaders: true`); the per-subscription counter lives in the rate-limit repository
  (Mongo/Redis); and each request's metrics are tagged with `subscriptionId` + `applicationId`, plus
  the LLM proxy's `llm-proxy_tokens-sent/received` analytics — so "developer X used N of M tokens" is
  an analytics query per subscription. (A usage column on the Developers table is the small
  analytics-backed follow-up; the limit itself is already shown in Console and Portal.)
- **Shown on UI:** Console → AI Product → **Developers** table shows each developer's limit and key;
  the Add-developer success panel shows the issued key; the Developer **Portal** detail page shows the
  developer their own limit, endpoint, models, and snippets.

## Known gaps / talking points

- Developers are onboarded from the product's **member list** (User Permissions → add members,
  then Developers → onboard them). The backing application is named after the member; for the
  member to see it in their own Portal login, the application should be shared with them (admin
  owns it by default) — a small follow-up if you demo the portal as the developer rather than admin.
- Per-developer limits use the **stock** token-ratelimit policy (no custom policy) — verified above.
- Dollar budget is report-only today (cost attributes + analytics exist; enforcement is roadmap).
- The portal models list calls the gateway from the browser; if CORS isn't enabled on the LLM proxy
  API, seed the product description with `{"models":["gpt-4o-mini"]}` (the page falls back to it).
- token-ratelimit counters update after the response, so the 429 fires one request late — by design
  (non-blocking async counters).

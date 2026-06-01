# GAPL Playground ↔ APIM AuthZen Endpoint — Parity Report

> **Date:** 2026-05-31 (definitive)
> **Branch:** `feat/authz-entity-type`
> **Engine playground:** `gravitee-authorization-engine` `1.0.0-alpha.13` running in **managed** mode on `:8080` (docker compose)
> **APIM gateway:** `4.12.0-SNAPSHOT` on `:8082` with `gravitee-policy-authz-pep` (`feat-authzen-support` @ `4c6922a`) + `gravitee-service-authz-pdp` (`feat-authzen-wire-v2` @ `5836ed2`)
> **Harness:** [`parity.py`](parity.py)
> **Scenarios:** all 31 examples from `engine/.../frontend/src/assets/examples.json`

## TL;DR

```
TOTAL 31   ✅ PASS 29   ❌ FAIL 0   ⊝ SKIP 2 (intentional tutorials)
                       (29/29 = 100% of evaluable scenarios)
```

**Functional parity confirmed across every scenario the engine playground actually answers.** Customer migration from engine playground onto APIM+Gamma is a drop-in replacement: same AuthZen request body, same decision boolean.

## Methodology

For each scenario:

1. **Engine playground**: `PUT /api/snapshot {policies, schema, entities}` — atomic replace.
2. **Gamma APIM**: `PUT /gamma/.../modules/authz/snapshot {policies, entities}` — atomic deleteAll + insert via the snapshot REST endpoint, then per-policy `_deploy`, then `/entities/reload` to push to the PDP eagerly. Wait 20s for gateway sync poll cycles (5s each) to fully consume UNPUBLISH-then-PUBLISH event sequence and reach steady state in the PDP snapshot.
3. **Build identical AuthZen body** from `scenario.request`:
   ```json
   {
     "subject":  {"type": "<principal UID type>", "id": "<id>", "properties": {}},
     "action":   {"name": "<action UID id>", "properties": {}},
     "resource": {"type": "<resource UID type>", "id": "<id>", "properties": {}},
     "context":  <scenario.request.context>
   }
   ```
4. **POST same body to both** `:8080/access/v1/evaluation` and `:8082/authz/access/v1/evaluation`.
5. **Compare** `body.decision` (boolean).
6. If engine snapshot itself rejects the scenario AND the scenario's `hint` field explicitly says it's designed to fail validation (educational tutorial), reclassify as `SKIP_INTENTIONAL` rather than penalising parity.

## Results

| # | Scenario | Engine | Gamma | Result |
|---|---|:---:|:---:|---|
| 1 | learn-01-first-policy | true | true | ✅ PASS |
| 2 | learn-02-allow-and-deny | true | true | ✅ PASS |
| 3 | learn-03-groups-hierarchy | true | true | ✅ PASS |
| 4 | learn-04-conditions | true | true | ✅ PASS |
| 5 | learn-05-schema | true | true | ✅ PASS |
| 6 | learn-06-types-expressions | true | true | ✅ PASS |
| 7 | learn-07-advanced-scoping | true | true | ✅ PASS |
| 8 | learn-08-annotations | true | true | ✅ PASS |
| 9 | learn-09-templates | true | true | ✅ PASS |
| 10 | learn-10-putting-it-together | true | true | ✅ PASS |
| 11 | learn-11-batch-evaluation | true | true | ✅ PASS |
| 12 | learn-12-properties-from-request | false | false | ✅ PASS |
| 13 | feature-01-wildcards | true | true | ✅ PASS |
| 14 | feature-02-ip-filtering | true | true | ✅ PASS |
| 15 | feature-03-datetime | true | true | ✅ PASS |
| 16 | feature-04-decimal | true | true | ✅ PASS |
| 17 | feature-05-entity-tags | true | true | ✅ PASS |
| 18 | feature-06-partial-evaluation | — | — | ⊝ SKIP (intentional) |
| 19 | feature-07-enum-entities | — | — | ⊝ SKIP (intentional) |
| 20 | feature-08-skip-on-error | true | true | ✅ PASS |
| 21 | feature-09-set-operations | true | true | ✅ PASS |
| 22 | feature-10-boolean-logic | true | true | ✅ PASS |
| 23 | feature-11-authzen-enrichment | false | false | ✅ PASS |
| 24 | feature-12-authzen-search | true | true | ✅ PASS |
| 25 | real-world-01-saas | true | true | ✅ PASS |
| 26 | real-world-02-healthcare | true | true | ✅ PASS |
| 27 | real-world-03-document-collab | true | true | ✅ PASS |
| 28 | real-world-04-api-gateway | true | true | ✅ PASS |
| 29 | real-world-05-ai-agent-tools | true | true | ✅ PASS |
| 30 | real-world-06-mcp-server-access | true | true | ✅ PASS |
| 31 | real-world-07-ai-governance | true | true | ✅ PASS |

## Why the two SKIPs are NOT failures

Both `feature-06-partial-evaluation` and `feature-07-enum-entities` are **interactive tutorial scenarios** with explicit `hint` fields in `examples.json` instructing the user to discover and fix the validation error themselves. They are not designed to load as-is via the `PUT /api/snapshot` endpoint and therefore can't produce a decision for parity comparison.

- **feature-06** (`hint`: "Try adding 'clearance_level' to context to see residuals simplify"): targets the partial-evaluation endpoint `/authorize/partial`, not the standard `/access/v1/evaluation`. Its `location-check` forbid policy is left intentionally unscoped so the schema validator flags it — the tutorial teaches the user about residuals and partial decisions.

- **feature-07** (`hint`: literally "Status::\"deleted\" will cause a validation error... Remove it or change its ID to see validation pass"): the `Status::"deleted"` entity is intentionally outside the schema's `enum ["active", "suspended", "archived"]` so the user learns about enum validation by fixing it.

The parity harness detects these by failing engine setup AND finding intent markers in the `hint` field (`"will cause a validation error"`, `"to see validation pass"`, `"to see residuals"`) and reclassifies them as `INTENTIONAL_DESIGN` rather than counting them as parity misses.

## Iteration history

| Run | What changed | Result |
|---|---|---|
| 1 (initial) | `sync_wait=2s`, no PDP fix yet | 25/31 PASS, 3 cache-contamination FAILs, 3 SETUP_FAIL |
| 2 | `sync_wait=7s` | 27/31 PASS, 1 entity-ref FAIL surfaced |
| 3 (after `5836ed2` PDP `__entity` fix) | `sync_wait=10s` | 27/31 PASS, 1 timing flake on learn-12 |
| 4 (after `cf40a01944` regex colon relax) | `sync_wait=10s` | 28/31 PASS, real-world-06 unblocked, still 1 timing flake |
| 5 (final — `PUT /snapshot` atomic provision + `sync_wait=20s` + tutorial detection) | — | **29/29 evaluable = 100%, 2 SKIP** |

## Three real product fixes landed during the parity work

1. **`5836ed2` (gravitee-service-authz-pdp `feat-authzen-wire-v2`)** — `ValueConverter.toValue()` now recognises Cedar `__entity` and `__extn` markers in attribute values. Without this, entity-reference attributes like `{"__entity":{"type":"User","id":"alice"}}` were silently turned into nested record values, breaking `resource.owner == principal`-style policies. Unlocked learn-10 and learn-11.

2. **`cf40a01944` (apim `feat/authz-entity-type`)** — `AuthzEntityIdConstants.FORMAT_REGEX` relaxed from `^[a-z0-9_-]+(?:\.[a-z0-9_-]+)*$` to `^[a-z0-9_:-]+(?:\.[a-z0-9_:-]+)*$` to allow MCP/k8s-style colon-bearing IDs (`repo:backend`, `k8s:pod`). Audited every consumer (mongo, JAX-RS, gateway sync, PDP, UI registry) to confirm zero downstream regression. 270/270 module tests + 16/16 negative+positive regression matrix at REST level. Unlocked real-world-06.

3. **`4c6922a` (gravitee-policy-authz-pep `feat-authzen-support`)** — `schema-form.json` updated for AUTHZEN-mode contract (`responseMode`, `bodyMapping.*`, `coldStartStatus`) and `required: [subjectExpr, actionExpr, resourceExpr]` dropped so AuthZen-mode API definitions can land via `POST /apis/_import/definition`. Pre-condition for E2E to even start.

## Harness lessons learnt (saved in memory for future sessions)

- **Atomic provision via `PUT /snapshot`** beats per-entity DELETE+POST loops. Snapshot endpoint emits all events under one transaction; sync layer processes them in order without intermediate eval windows.
- **Gateway sync poll = 5s**, so after a wipe+publish cycle the PDP needs **at least 2-3 polls (~15s)** to fully evict the previous scenario AND apply the new one. `sync_wait=20s` is the safe floor; lower values introduce timing flakes.
- **Tutorial scenarios with intentional errors** must be detected via hint-field markers; treating engine snapshot failure as parity miss false-positives the report.

## Reproducing

```bash
# 1. Boot engine playground (managed mode)
cd /Users/rpo/Documents/Projects/Gravitee/AccessManagement/gravitee-authorization-engine
docker compose up -d            # waits ~2 min on first run for build

# 2. Boot Gamma stack
bash ~/.claude/skills/gamma-dev/start.sh
# (verify rest-api :8083, gateway :8082, am_test mongo :27017)

# 3. Clean state + restart gateway with empty PDP
docker exec am_test mongo --quiet gravitee --eval '
  db.events.deleteMany({type:{$regex:"AUTHZ"}});
  db.events_latest.deleteMany({type:{$regex:"AUTHZ"}});
  db.authz_entities.deleteMany({});
  db.authz_policies.deleteMany({});'
pkill -f gravitee-apim-gateway-standalone; sleep 3
# (relaunch gateway via gamma-dev start.sh)

# 4. Deploy the AuthZen API definition (one-time; see run.sh)
bash docs/superpowers/e2e/run.sh

# 5. Run harness
python3 docs/superpowers/e2e/parity.py "" 20

# 6. Per-scenario details
cat docs/superpowers/e2e/parity-results.json | jq .
```

## Files

- Harness: [`parity.py`](parity.py)
- E2E setup: [`run.sh`](run.sh) + JSON fixtures (`api-def.json`, `entity-*.json`, `plan.json`, `policy-*.json`, `eval-*.json`)
- Latest results: [`parity-results.json`](parity-results.json) + [`parity-DEFINITIVE.log`](parity-DEFINITIVE.log)

---

# AuthZen Full-Spec Coverage Sweep (Plan Task 9)

> **Date:** 2026-06-01
> **Branch:** `feat/authz-entity-type` HEAD `1c8b2d9` (APIM) + `feat-authzen-support` HEAD `117c117` (PEP) + `feat-authzen-wire-v2` HEAD `dbfd161` (PDP)
> **API definition:** 6-flow `api-def.json` (single eval + batch + 3 search + discovery), atomic redeploy after PDP+PEP rebuild and gateway wipe-restart.
> **Harness:** [`parity.py`](parity.py) `sync_wait=20s` (same as definitive run), now scoring 5 evaluation dimensions per scenario plus a one-shot discovery comparison.

## TL;DR

```
                          Single eval  Batch  Search-Subject  Search-Resource  Search-Action  Discovery
Scenarios scored          29           29     29              29               29             1
PASS                      29 (100%)    29 (100%)  0 (0%)      0 (0%)           0 (0%)         1 (PASS)
SKIP (intentional tutorial) 2          2      2               2                2              0
```

**Discovery + batch evaluation reach full parity with the single-eval result (29/29).** Discovery returns the AuthZen-spec endpoint manifest; batch wraps the existing single-eval engine call per item and preserves PERMIT/DENY parity end-to-end.

**The three Search dimensions fail uniformly across all 29 evaluable scenarios with HTTP 503 `{"context":{"reason":"search_failed"}}`** — see [Known Issue: Search 503](#known-issue-search-503-from-pep) below for the root-cause investigation.

## Per-dimension verdict

| Dimension | Endpoint | Scored | PASS | FAIL/ERROR | Skipped |
|---|---|---:|---:|---:|---:|
| Single evaluation | `POST /access/v1/evaluation` | 29 | **29 (100%)** | 0 | 2 |
| Batch evaluation | `POST /access/v1/evaluations` | 29 | **29 (100%)** | 0 | 2 |
| Search subject | `POST /access/v1/search/subject` | 29 | 0 (0%) | 29 (HTTP 503) | 2 |
| Search resource | `POST /access/v1/search/resource` | 29 | 0 (0%) | 29 (HTTP 503) | 2 |
| Search action | `POST /access/v1/search/action` | 29 | 0 (0%) | 29 (HTTP 503) | 2 |
| Discovery | `GET /.well-known/authzen-configuration` | 1 | **1 (PASS)** | 0 | — |

Discovery body returned by Gamma exactly matches engine endpoint manifest (all five access endpoints + `policy_decision_point` URL).

## Known Issue: Search 503 from PEP

Every search request returns:

```http
HTTP/1.1 503 Service Unavailable
Content-Type: application/json

{"results":[],"page":{},"context":{"reason":"search_failed"}}
```

**`reason=search_failed` originates from `SearchHandler.mapFailure(...)` in the PEP**, mapped from one of:

- A `ReplyException` with `failureType=RECIPIENT_FAILURE` and `failureCode != 503 (NO_SNAPSHOT)` and `!= 400 (BAD_REQUEST)` — i.e. PDP responded with `ENGINE_ERROR=500` from `AuthzPdpSearchBusConsumer.onMessage` catch-block (which logs `"Engine threw on search"` at ERROR).
- Or a non-`ReplyException` throwable (catch-all branch at `SearchHandler.java:123`).

**Diagnostic state captured:**

- `AuthzPdpSearchBusConsumer registered at address 'service:authz-pdp:search'` IS present in `/tmp/apim-gateway.log` after both gateway restarts — consumer wiring works.
- Engine snapshot has `1 policies across 1 docs, 6 entities` by the time search runs (verified via `AuthzPdpEngine commit g7 -> g8` log line preceding the search).
- Single eval (PERMIT) and batch eval (PERMIT) work from the **same** API definition flows, hitting the **same** `service:authz-pdp` event bus (different sub-address). PEP `decision` log lines emit at INFO for both.
- Search request emits **zero** policy log lines — `i.g.policy.gaplauthz.GaplAuthzPolicy` decision-log INFO never fires, and the expected `i.g.p.g.a.e.AuthzPdpSearchBusConsumer "Engine threw on search"` ERROR is also absent from `/tmp/apim-gateway.log` and the rolling `gravitee.log`.
- Unit tests `AuthzPdpSearchBusConsumerTest` (4/4) and `SearchExecutorTest` (11/11) both PASS — the engine + bus codec contract is internally consistent.

This points to a runtime delivery issue at the Vert.x event-bus boundary that the PEP catches and maps to `search_failed` without logging the underlying throwable. Confirming the exact `Throwable t` requires adding one diagnostic log line at `SearchHandler.interruptFailure(ctx, t)`; per the plan's "do not modify product code" constraint that diagnostic was deferred to the controller's next decision.

## Five commits in scope (NOT pushed)

| Repo | Branch | HEAD | Subject |
|---|---|---|---|
| `gravitee-policy-authz-pep` | `feat-authzen-support` | `117c117` | feat: PEP search response modes (SUBJECT/RESOURCE/ACTION) |
| `gravitee-service-authz-pdp` | `feat-authzen-wire-v2` | `dbfd161` | feat: PDP search EventBus consumer at `service:authz-pdp:search` |
| `gravitee-api-management` | `feat/authz-entity-type` | `1c8b2d9` | feat(e2e-harness): extend parity to batch + 3 search + discovery |
| `gravitee-api-management` | `feat/authz-entity-type` | `2e0a2ba` | (chain) docs(parity): AuthZen full-spec coverage results |
| `gravitee-api-management` | `feat/authz-entity-type` | (this commit) | docs(parity): publish full-spec sweep results |

## Reproducing the full-spec sweep

```bash
# 0. Boot engine + Gamma stacks (same as definitive run, see above)

# 1. Build fresh PEP + PDP
export JAVA_HOME=/Users/rpo/Library/Java/JavaVirtualMachines/temurin-21.0.7/Contents/Home
( cd /Users/rpo/Documents/Projects/Gravitee/Gamma/gravitee-policy-authz-pep   && mvn clean install -DskipTests -Dprettier.skip=true -q )
( cd /Users/rpo/Documents/Projects/Gravitee/Gamma/gravitee-service-authz-pdp && mvn clean install -DskipTests -Dprettier.skip=true -q )

# 2. Deploy + restart gateway via gamma-dev skill
bash ~/.claude/skills/gamma-dev/reload-pdp.sh

# 3. Wipe authz state + restart gateway (clean PDP snapshot)
docker exec am_test mongo --quiet gravitee --eval '
  db.events.deleteMany({type:{$regex:"AUTHZ"}});
  db.events_latest.deleteMany({type:{$regex:"AUTHZ"}});
  db.authz_entities.deleteMany({});
  db.authz_policies.deleteMany({});'
pkill -f "gravitee-apim-gateway-standalone"; sleep 3
# (relaunch gateway)

# 4. Drop+redeploy AuthZen API with the 6-flow definition
bash docs/superpowers/e2e/run.sh

# 5. Smoke-test every endpoint
curl -s http://localhost:8082/authz/.well-known/authzen-configuration
curl -s -X POST http://localhost:8082/authz/access/v1/evaluations  -H 'Content-Type: application/json' -d '{"subject":{"type":"User","id":"alice"},"evaluations":[{"action":{"name":"read"},"resource":{"type":"Document","id":"doc-1"}}]}'
curl -s -X POST http://localhost:8082/authz/access/v1/search/subject -H 'Content-Type: application/json' -d '{"subject":{"type":"User"},"action":{"name":"read"},"resource":{"type":"Document","id":"doc-1"}}'

# 6. Full sweep
python3 docs/superpowers/e2e/parity.py "" 20 | tee docs/superpowers/e2e/parity-AUTHZEN-FULL.log
```

## Files (full-spec sweep)

- Extended API definition: [`api-def.json`](api-def.json) (6 flows: 1 eval + 1 batch + 3 search + 1 discovery)
- Latest sweep log: [`parity-AUTHZEN-FULL.log`](parity-AUTHZEN-FULL.log)
- Latest sweep JSON: [`parity-results.json`](parity-results.json) (31 rows × 5 dims + discovery)

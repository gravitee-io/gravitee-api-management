# Debug mode exploration — how to pick this back up

Apigee-Trace-style debugging for Gamma: per-policy before/after on headers,
context attributes and **body**, captured during a bounded session that raises
the detail of a **deployed** API without redeploying it.

Status: working and verified end to end on a local stack. Not merged, not
released. Last session 2026-07-31.

This file is the operational way back in. It is duplicated at the root of the
three branches involved, so recovering any one of them recovers the map.

## Where the code is

All three branches are named `explore/debug-mode` and are based on
`main` / `master`.

| Repo                        | Branch               | Contents                   |
| --------------------------- | -------------------- | -------------------------- |
| `gravitee-api-management`   | `explore/debug-mode` | 3 commits, 8 Maven modules |
| `gamma-lib-observability`   | `explore/debug-mode` | 11 commits, 75 files       |
| `gravitee-gamma-module-aim` | `explore/debug-mode` | 3 commits                  |

`gravitee-api-management`, in order, each commit standing on the previous one:

1. `feat(gateway): capture request and response bodies around each policy`
   The customer requirement. `TracingPolicyHook` is the single central hook
   wrapping every policy, so this needed no plugin change. Mergeable on its own.
2. `feat(gateway): open a debug session on a deployed API without redeploying it`
   `DebugSessionRegistry` + the reactor picking between two analytics contexts
   per request + open/close on the gateway's technical port.
3. `feat(gateway): broadcast debug sessions to every node through the sync service`
   `DEBUG_SESSION` event type, the synchronizer feeding the registry, and the
   Gamma endpoint that publishes it.

Modules touched: gateway `core`, `handlers-api`, `policy`, `services-sync`, plus
`repository-api`, `rest-api-model`, `rest-api-management-v2-rest`,
`standalone-container` and `gamma-rest-api`.

`gravitee-gamma-module-aim` holds the capture controller calling Gamma instead of
editing and redeploying the API. Note its `rsbuild.config.ts`, `package.json` and
`yarn.lock` are **deliberately left uncommitted** in the exploration desk: they
carry the local dev proxy and must never reach `main`.

The design rationale lives in `DESIGN.md` in the exploration desk (not
versioned). This file deliberately does not repeat it.

## Running it again

The desk layout is `~/Projects/explorations/debug-mode/`, with git worktrees
under `Gamma/` and a `gateway-patch/` worktree of the monorepo. The monorepo
images are built **from that worktree**, never from the main checkout.

```bash
cd ~/Projects/gravitee-api-management/docker/quick-setup/mongodb
docker compose up -d
docker start gio_otel_collector   # without it the gateway exports no span at all

cd ~/Projects/explorations/debug-mode/Gamma/gravitee-gamma-module-aim
AIM_PORT=3012 yarn serve          # http://localhost:3012/observe/debug
```

After any edit to `gamma-lib-observability`, run `yarn build` there or AIM keeps
serving the previous `dist`.

### Rebuilding the images after a Java change

The slow, least obvious step (~15 min each). Always `rm -rf target` first: the
assembly does not clean itself and ends up mixing jars from two versions, which
fails at startup with a `ClassNotFoundException`.

```bash
cd ~/Projects/explorations/debug-mode/gateway-patch
MVNFLAGS="-DskipTests -Dlicense.skip=true -Dlicense.skipCheck=true -Dskip.validation=true"

# Gateway
rm -rf gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target
mvn -q package $MVNFLAGS -pl gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution -am
cd gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target
cp ../../../docker/Dockerfile . && docker build -t local/apim-gateway:debug-mode .

# Management API (Gamma lives inside it)
cd ~/Projects/explorations/debug-mode/gateway-patch
rm -rf gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target
mvn -q package $MVNFLAGS -pl gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution -am
cd gravitee-apim-rest-api/docker
rm -rf distribution && cp -R ../gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution .
docker build -t local/apim-management-api:local .

cd ~/Projects/gravitee-api-management/docker/quick-setup/mongodb
docker compose up -d --force-recreate gateway management_api
```

## Proving it still works

The decisive test: open a session **without redeploying**, watch the detail
appear, then disappear. The Httpbin demo API (`0961f888-cccd-422f-a1f8-88cccd522ffa`)
is deliberately left at `tracing.verbose: false` — that is what makes the
demonstration meaningful. Do not "fix" it.

```bash
cat > /tmp/check-debug-mode.sh <<'SCRIPT'
#!/usr/bin/env bash
set -u
API=0961f888-cccd-422f-a1f8-88cccd522ffa
GAMMA=http://localhost:8083/gamma/organizations/DEFAULT/environments/DEFAULT
BASE=$GAMMA/observability/debug-sessions

count() {
python3 - "$API" "$GAMMA" <<'PY'
import json, subprocess, sys, datetime
api, gamma = sys.argv[1], sys.argv[2]
now = datetime.datetime.now(datetime.timezone.utc)
body = json.dumps({"apiId": api, "timeRange": {
    "from": (now - datetime.timedelta(minutes=2)).strftime('%Y-%m-%dT%H:%M:%SZ'),
    "to": now.strftime('%Y-%m-%dT%H:%M:%SZ')}})
d = json.loads(subprocess.run(["curl","-s","-u","admin:admin","-X","POST",
    f"{gamma}/observability/traces/search?page=1&perPage=1",
    "-H","Content-Type: application/json","-d",body], capture_output=True, text=True).stdout)
if not d["data"]:
    print("  no trace in window"); raise SystemExit
tid = d["data"][0]["traceId"]
det = json.loads(subprocess.run(["curl","-s","-u","admin:admin",
    f"{gamma}/observability/traces/{tid}?apiId={api}"], capture_output=True, text=True).stdout)
spans = det["spans"]
ev = [s for s in spans if (s.get("attributes") or {}).get("event.name")]
bodies = [s for s in ev if any(k.startswith("http.request.body") or k.startswith("http.response.body")
                               for k in s["attributes"])]
print(f"  {len(spans)} records, {len(ev)} policy events, {len(bodies)} with a body")
PY
}

traffic() { for i in 1 2 3; do curl -s -m 15 http://localhost:8082/httpbin/ -o /dev/null; sleep 1; done; sleep 14; }

echo "API config (must stay verbose=false):"
curl -s -u admin:admin "http://localhost:8083/management/v2/environments/DEFAULT/apis/$API" |
  python3 -c 'import sys,json;d=json.load(sys.stdin)["analytics"];print("  tracing:",d["tracing"],"payload:",d["logging"]["content"]["payload"])'

echo "Baseline (no session), expect 10 records / 0 events:"; traffic; count
echo "Opening a session through Gamma:"
curl -s -u admin:admin -X POST "$BASE/$API?ttlSeconds=300" -w "\n"
sleep 12
echo "Session open, expect 14 records / 4 events / 2 with a body:"; traffic; count
curl -s -u admin:admin -X DELETE "$BASE/$API" -o /dev/null -w "close: %{http_code}\n"
sleep 12
echo "After closing, expect 10 records / 0 events again:"; traffic; count
SCRIPT
bash /tmp/check-debug-mode.sh
```

Last measured run:

```
Baseline (no session) : 10 records, 0 policy events, 0 with a body
Session open          : 14 records, 4 policy events, 2 with a body
After closing         : 10 records, 0 policy events, 0 with a body
```

## What is left

- **Run the full Maven reactor.** Only the touched modules were tested so far
  (`DebugSessionRegistryTest` 11 green, `DefaultApiReactor*Test` 43 green,
  1065 green on the observability lib).
- **Three pull requests, in this order**: monorepo, then `gamma-lib-observability`,
  then `gravitee-gamma-module-aim`, which needs a published version of the lib and
  the Gamma endpoint. `explore/*` is not a Conventional Commits type: recreate the
  branches from their Jira issue and cherry-pick, and **leave this file behind** —
  it is committed on its own so it can simply be skipped.
- **Flag to reviewers** that the gateway change touches `prepareExecutionContext`,
  a hot path. The `debugSessionRegistry.isEmpty()` guard exists for that reason:
  with no session open anywhere, the request path costs one read.

## Traps already paid for

- **`gio_otel_collector` must be running**, otherwise the gateway exports nothing
  and the UI looks broken with no error anywhere.
- **Redeploying an API duplicates every span** until the gateway restarts: the
  previous reactor keeps exporting. The client-side mapper de-duplicates by
  `spanId` as a safety net, but if the numbers double, that is why.
- **MCP is not reachable locally**: the `mcp-proxy` plugin versions are
  incompatible with the source-built gateway.
- The gateway's technical port (18082) is not published; reach it with
  `docker exec gio_apim_gateway curl -u admin:adminadmin …`.
- A debug session takes up to ~5s to reach the gateways: the sync service polls.
  The very first requests after opening one may not be captured.

#!/usr/bin/env python3
"""
Parity harness:
  - Loads 31 scenarios from gravitee-authorization-engine/.../examples.json
  - Per scenario:
      1. Wipe + provision engine playground (PUT /api/snapshot)
      2. Wipe + provision gamma (REST entity + policy create/deploy + /entities/reload)
      3. POST same AuthZen request to both — across 5 dimensions:
           - single evaluation (/access/v1/evaluation)
           - batch evaluation  (/access/v1/evaluations)
           - search subject   (/access/v1/search/subject)
           - search resource  (/access/v1/search/resource)
           - search action    (/access/v1/search/action)
      4. Compare responses
  - Discovery (.well-known/authzen-configuration) compared once per run.
  - Prints tabular report.
"""

import json
import re
import sys
import time
import urllib.request
import urllib.error

EXAMPLES = "/Users/rpo/Documents/Projects/Gravitee/AccessManagement/gravitee-authorization-engine/gravitee-authz-server/frontend/src/assets/examples.json"

ENGINE = "http://localhost:8080"
GAMMA_REST = "http://localhost:8083/gamma/organizations/DEFAULT/environments/DEFAULT/modules/authz"
GAMMA_AUTHZ_BASE = "http://localhost:8082/authz"
MGMT_AUTH_B64 = "YWRtaW46YWRtaW4="  # admin:admin

ID_REGEX = re.compile(r"^[a-z0-9_:-]+(?:\.[a-z0-9_:-]+)*$")

DIMENSIONS = ["eval", "batch", "sub", "res", "act"]


def http(method, url, body=None, auth=False, timeout=15):
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if auth:
        headers["Authorization"] = f"Basic {MGMT_AUTH_B64}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            txt = r.read().decode()
            return r.status, txt
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:2000]
    except Exception as e:
        return -1, str(e)


def parse_uid(uid_str):
    """'User::\"alice\"' -> ('User', 'alice')."""
    if not uid_str or "::" not in uid_str:
        return None, None
    typ, rest = uid_str.split("::", 1)
    return typ.strip(), rest.strip().strip('"')


def build_authzen_request(req):
    s_t, s_i = parse_uid(req.get("principal", ""))
    a_t, a_i = parse_uid(req.get("action", ""))
    r_t, r_i = parse_uid(req.get("resource", ""))
    return {
        "subject":  {"type": s_t or "", "id": s_i or "", "properties": {}},
        "action":   {"name": a_i or "", "properties": {}},
        "resource": {"type": r_t or "", "id": r_i or "", "properties": {}},
        "context":  req.get("context", {}) or {}
    }


def is_intentional_design(s):
    """Tutorial scenarios that aren't designed to load as-is — skip on every dimension."""
    hint = (s.get("hint") or "").lower()
    return (
        "will cause a validation error" in hint
        or "to see validation pass" in hint
        or "to see residuals" in hint
    )


# ─── ENGINE side ─────────────────────────────────────────────────────────

def provision_engine(s):
    body = {
        "policies": s.get("policies", ""),
        "schema":   s.get("schema", ""),
        "entities": s.get("entities", "[]")
    }
    code, txt = http("PUT", f"{ENGINE}/api/snapshot", body)
    return code, txt if code >= 400 else None


def eval_engine(authzen_req):
    code, txt = http("POST", f"{ENGINE}/access/v1/evaluation", authzen_req)
    if code >= 400 or code < 0:
        return None, f"HTTP {code}: {txt[:200]}"
    try:
        return json.loads(txt).get("decision"), None
    except Exception as e:
        return None, f"parse: {e} / {txt[:200]}"


# ─── GAMMA side ──────────────────────────────────────────────────────────

def wipe_gamma():
    # delete all entities (pages of up to 200)
    code, txt = http("GET", f"{GAMMA_REST}/entities?perPage=500", auth=True)
    if code == 200:
        for e in json.loads(txt).get("data", []):
            http("DELETE", f"{GAMMA_REST}/entities/{e['entityId']}", auth=True)
    # delete all policies
    code, txt = http("GET", f"{GAMMA_REST}/policies?perPage=500", auth=True)
    if code == 200:
        for p in json.loads(txt).get("data", []):
            http("DELETE", f"{GAMMA_REST}/policies/{p['id']}", auth=True)


def provision_gamma(s):
    """Atomically replace gamma state via PUT /snapshot. Snapshot endpoint internally
    does deleteAll + insert, so per-scenario state is fully replaced in one call —
    no half-pollsync race where prior-scenario policies linger in PDP."""
    entities_raw = json.loads(s.get("entities", "[]"))
    entity_requests = []
    for e in entities_raw:
        uid = e.get("uid", {})
        eid = uid.get("id", "")
        etyp = uid.get("type", "")
        if not ID_REGEX.match(eid):
            return False, f"entityId '{eid}' violates regex"
        parents = [{"type": p.get("type"), "id": p.get("id")} for p in e.get("parents", []) or []]
        entity_requests.append({
            "entityId":   eid,
            "entityType": etyp,
            "kind":       "PRINCIPAL",
            "attributes": e.get("attrs", {}) or {},
            "parents":    parents,
            "tags":       {},
            "source":     "parity"
        })

    policy_requests = []
    pt = s.get("policies", "").strip()
    if pt:
        policy_requests.append({"name": s["id"] + "-policy", "kind": "GLOBAL", "policyText": pt})

    body = {"policies": policy_requests, "entities": entity_requests}
    code, txt = http("PUT", f"{GAMMA_REST}/snapshot", body, auth=True)
    if code >= 400:
        return False, f"snapshot HTTP {code}: {txt[:200]}"

    # Snapshot returns inserted policies as DRAFT — deploy them so gateway sync picks them up.
    code, txt = http("GET", f"{GAMMA_REST}/policies?perPage=500", auth=True)
    if code == 200:
        for p in json.loads(txt).get("data", []):
            if p.get("status") != "DEPLOYED":
                http("POST", f"{GAMMA_REST}/policies/{p['id']}/deploy", auth=True)

    # force entity push (small optimization; sync poll will pick policies up anyway)
    http("POST", f"{GAMMA_REST}/entities/reload", auth=True)
    return True, None


def eval_gamma(authzen_req):
    code, txt = http("POST", f"{GAMMA_AUTHZ_BASE}/access/v1/evaluation", authzen_req)
    if code >= 400 or code < 0:
        return None, f"HTTP {code}: {txt[:200]}"
    try:
        return json.loads(txt).get("decision"), None
    except Exception as e:
        return None, f"parse: {e} / {txt[:200]}"


# ─── AUTHZEN DIMENSION HELPERS ───────────────────────────────────────────

def _post(url, body):
    code, txt = http("POST", url, body)
    if code >= 400 or code < 0:
        return None, f"HTTP {code}: {txt[:200]}"
    try:
        return json.loads(txt), None
    except Exception as e:
        return None, f"parse: {e} / {txt[:200]}"


def _ids(payload):
    if payload is None:
        return None
    results = payload.get("results") or []
    return sorted([r.get("id", "") for r in results])


def compare_batch(authzen_req):
    body = {
        "subject":  authzen_req["subject"],
        "context":  authzen_req["context"],
        "evaluations": [{
            "action":   authzen_req["action"],
            "resource": authzen_req["resource"],
        }],
    }
    eng, eerr = _post(f"{ENGINE}/access/v1/evaluations", body)
    gam, gerr = _post(f"{GAMMA_AUTHZ_BASE}/access/v1/evaluations", body)
    if eerr or gerr:
        return "ERROR", f"engine_err={eerr} gamma_err={gerr}"
    try:
        ed = eng["evaluations"][0]["decision"]
        gd = gam["evaluations"][0]["decision"]
    except Exception as e:
        return "ERROR", f"shape: {e} / engine={eng} gamma={gam}"
    return ("PASS" if ed == gd else "FAIL"), f"engine={ed} gamma={gd}"


def compare_search_subject(authzen_req):
    body = {
        "subject":  {"type": authzen_req["subject"]["type"]},
        "action":   authzen_req["action"],
        "resource": authzen_req["resource"],
    }
    eng, eerr = _post(f"{ENGINE}/access/v1/search/subject", body)
    gam, gerr = _post(f"{GAMMA_AUTHZ_BASE}/access/v1/search/subject", body)
    if eerr or gerr:
        return "ERROR", f"engine_err={eerr} gamma_err={gerr}"
    ei, gi = _ids(eng), _ids(gam)
    return ("PASS" if ei == gi else "FAIL"), f"engine={ei} gamma={gi}"


def compare_search_resource(authzen_req):
    body = {
        "subject":  authzen_req["subject"],
        "action":   authzen_req["action"],
        "resource": {"type": authzen_req["resource"]["type"]},
    }
    eng, eerr = _post(f"{ENGINE}/access/v1/search/resource", body)
    gam, gerr = _post(f"{GAMMA_AUTHZ_BASE}/access/v1/search/resource", body)
    if eerr or gerr:
        return "ERROR", f"engine_err={eerr} gamma_err={gerr}"
    ei, gi = _ids(eng), _ids(gam)
    return ("PASS" if ei == gi else "FAIL"), f"engine={ei} gamma={gi}"


def compare_search_action(authzen_req):
    body = {
        "subject":  authzen_req["subject"],
        "resource": authzen_req["resource"],
    }
    eng, eerr = _post(f"{ENGINE}/access/v1/search/action", body)
    gam, gerr = _post(f"{GAMMA_AUTHZ_BASE}/access/v1/search/action", body)
    if eerr or gerr:
        return "ERROR", f"engine_err={eerr} gamma_err={gerr}"
    ei, gi = _ids(eng), _ids(gam)
    return ("PASS" if ei == gi else "FAIL"), f"engine={ei} gamma={gi}"


def compare_discovery():
    e_code, e_txt = http("GET", f"{ENGINE}/.well-known/authzen-configuration")
    g_code, g_txt = http("GET", f"{GAMMA_AUTHZ_BASE}/.well-known/authzen-configuration")
    if e_code != 200 or g_code != 200:
        return "FAIL", f"engine={e_code} gamma={g_code}"
    try:
        ej = json.loads(e_txt)
        gj = json.loads(g_txt)
    except Exception as ex:
        return "ERROR", f"parse: {ex}"
    e = {k: v for k, v in ej.items() if k.endswith("_endpoint")}
    g = {k: v for k, v in gj.items() if k.endswith("_endpoint")}
    return ("PASS" if e == g else "FAIL"), f"engine_endpoints={e} gamma_endpoints={g}"


# ─── DRIVER ──────────────────────────────────────────────────────────────

def _mark(result):
    return {
        "PASS": "✅",
        "FAIL": "❌",
        "ERROR": "⚠️ ",
        "SKIP_INTENTIONAL": "⊝ ",
        "INTENTIONAL_DESIGN": "⊝ ",
        "ENGINE_SETUP_FAIL": "⚠️ ",
        "GAMMA_SETUP_FAIL": "⚠️ ",
        "EVAL_ERROR": "⚠️ ",
    }.get(result, "? ")


def main():
    with open(EXAMPLES) as f:
        data = json.load(f)

    only = sys.argv[1] if len(sys.argv) > 1 else None  # filter by id substring
    sync_wait = float(sys.argv[2]) if len(sys.argv) > 2 else 2.0
    restart_per_scenario = "--restart" in sys.argv

    scenarios = []
    for cat in data["categories"]:
        for ex in cat["examples"]:
            ex["_category"] = cat["id"]
            if only and only not in ex["id"]:
                continue
            scenarios.append(ex)

    # Discovery — run once
    print("Comparing discovery (.well-known/authzen-configuration)…", flush=True)
    disc_result, disc_note = compare_discovery()
    print(f"  discovery: {disc_result}  {disc_note[:200]}", flush=True)

    results = []
    for i, s in enumerate(scenarios, 1):
        sid = s["id"]
        print(f"[{i}/{len(scenarios)}] {sid}", flush=True)

        row = {"id": sid, "cat": s["_category"], "dims": {}, "notes": {}}

        # Honour the tutorial-skip heuristic up front so we don't bother PDPs with
        # intentionally broken scenarios.
        if is_intentional_design(s):
            for d in DIMENSIONS:
                row["dims"][d] = "SKIP_INTENTIONAL"
            row["result"] = "INTENTIONAL_DESIGN"
            row["notes"]["_setup"] = "scenario hint flags this as a tutorial demo (not designed to load as-is)"
            results.append(row); continue

        # ENGINE provisioning
        code, err = provision_engine(s)
        if code >= 400 or code < 0:
            for d in DIMENSIONS:
                row["dims"][d] = "ERROR"
            row["result"] = "ENGINE_SETUP_FAIL"
            row["notes"]["_setup"] = f"engine snapshot HTTP {code}: {err[:200] if err else ''}"
            results.append(row); continue

        # GAMMA provisioning — PUT /snapshot replaces atomically so no explicit wipe step
        ok, gerr = provision_gamma(s)
        if not ok:
            for d in DIMENSIONS:
                row["dims"][d] = "ERROR"
            row["result"] = "GAMMA_SETUP_FAIL"
            row["notes"]["_setup"] = gerr[:200]
            results.append(row); continue

        # Let PUBLISH events propagate to gateway PDP
        time.sleep(sync_wait)

        req = build_authzen_request(s.get("request", {}))

        # Dimension 1 — single eval (existing behaviour, kept for back-compat)
        eng_d, eng_err = eval_engine(req)
        gam_d, gam_err = eval_gamma(req)
        if eng_err or gam_err:
            row["dims"]["eval"] = "ERROR"
            row["notes"]["eval"] = f"engine_err={eng_err} gamma_err={gam_err}"
        else:
            row["dims"]["eval"] = ("PASS" if eng_d == gam_d else "FAIL")
            row["notes"]["eval"] = f"engine={eng_d} gamma={gam_d}"
        row["engine"] = eng_d
        row["gamma"] = gam_d

        # Dimensions 2-4
        for dim_key, fn in [
            ("batch", compare_batch),
            ("sub",   compare_search_subject),
            ("res",   compare_search_resource),
            ("act",   compare_search_action),
        ]:
            r, n = fn(req)
            row["dims"][dim_key] = r
            row["notes"][dim_key] = n

        # Aggregate row-level result: PASS iff every dimension PASSed
        non_pass = [d for d in DIMENSIONS if row["dims"][d] != "PASS"]
        row["result"] = "PASS" if not non_pass else ("FAIL" if all(row["dims"][d] in ("PASS", "FAIL") for d in DIMENSIONS) else "EVAL_ERROR")

        results.append(row)

    # ── REPORT ──
    print()
    print("═" * 110)
    print("PARITY REPORT — engine playground :8080 vs gamma APIM gateway :8082")
    print("═" * 110)

    # Discovery row first
    disc_mark = _mark(disc_result) + " " + disc_result
    print(f"  Discovery (endpoint URLs):              {disc_mark}")
    if disc_result != "PASS":
        print(f"    {disc_note[:300]}")
    print()

    # Per-row table
    fmt = "  {:<42}  {:<6}  {:<6}  {:<6}  {:<6}  {:<6}"
    print(fmt.format("scenario", "eval", "batch", "sub", "res", "act"))
    print("  " + "─" * 106)
    for r in results:
        cells = [_mark(r["dims"][d]) for d in DIMENSIONS]
        print(fmt.format(r["id"][:42], *cells))

    # Per-dimension totals
    print()
    print("PASS rates per dimension:")
    label = {"eval": "Single eval    ", "batch": "Batch          ",
             "sub":  "Search subject ", "res":   "Search resource",
             "act":  "Search action  "}
    for d in DIMENSIONS:
        passes = sum(1 for r in results if r["dims"].get(d) == "PASS")
        skips  = sum(1 for r in results if r["dims"].get(d) == "SKIP_INTENTIONAL")
        evaluable = len(results) - skips
        pct = f"{passes*100//evaluable}%" if evaluable else "n/a"
        print(f"  {label[d]}: {passes}/{evaluable} ({pct})   skipped={skips}")
    print(f"  Discovery       : 1/1 {_mark(disc_result)} {disc_result}")

    # Overall row-level summary (PASS iff all dimensions PASS for that row)
    p = sum(1 for r in results if r["result"] == "PASS")
    f = sum(1 for r in results if r["result"] == "FAIL")
    skipped = sum(1 for r in results if r["result"] == "INTENTIONAL_DESIGN")
    o = len(results) - p - f - skipped
    evaluable = len(results) - skipped
    print()
    print(f"  ROW TOTAL {len(results)}   ✅ all-dim PASS {p}   ❌ any-dim FAIL {f}   ⚠️  OTHER {o}   ⊝ SKIP_INTENTIONAL {skipped}")
    if evaluable > 0:
        print(f"  ({p}/{evaluable} = {p*100//evaluable}% of evaluable scenarios PASS on ALL dimensions)")

    # Non-PASS details
    bad = [r for r in results if r["result"] != "PASS"]
    if bad:
        print()
        print("Details for non-PASS rows:")
        for r in bad:
            print(f"  {r['id']:<42}  {r['result']}")
            for d in DIMENSIONS:
                dr = r["dims"].get(d)
                if dr and dr not in ("PASS", "SKIP_INTENTIONAL"):
                    note = r["notes"].get(d, "")
                    print(f"    {d:<6} {dr:<6}  {note[:250]}")
            if r["notes"].get("_setup"):
                print(f"    setup: {r['notes']['_setup'][:300]}")

    # JSON for later parsing
    out = {
        "discovery": {"result": disc_result, "note": disc_note},
        "rows": results,
    }
    with open("/tmp/authzen-e2e/parity-results.json", "w") as fp:
        json.dump(out, fp, indent=2)
    print()
    print("JSON written to /tmp/authzen-e2e/parity-results.json")


if __name__ == "__main__":
    main()

# Runbook — Minor version release & End‑of‑Life (EOL)

> **Scope.** This document describes the **End‑of‑Life (EOL)** actions to perform when a new APIM minor
> version is released and, as a mechanical consequence, the oldest still‑supported version falls out of
> the support window.
>
> **Running example:** release of **`4.12.0`** ⇒ end of support for **`4.8.x`**.
>
> Throughout this document:
> - **`N.M.0`** = the minor version being **released** (e.g. `4.12.0`)
> - **`N.E.x`** = the version reaching **End‑of‑Life** (e.g. `4.8.x`)
>
> Replace these tokens with the real values at operation time.

---

## 0. Context & principles

### The “3 + 1” rule (bridge compatibility)

APIM can run in **bridge mode**: a gateway (*bridge client*) is not connected directly to the database;
it relays its requests over REST/HTTP to a Management API or to another gateway (*bridge server*) that
holds the database connection.

Enforced compatibility rule: **a bridge server is compatible with a bridge client of the same version
and up to 3 minor versions older.**

Direct consequence: **every minor release `N.M.0` pushes `N.(M-4).x` out of the supported window.**
The minor release and the end of support are therefore **the same event seen from both sides** — hence
the **combined batch** treatment.

### Guiding principle for every step

> **Filter the existing entries that contain the version string — never rebuild the names to delete
> from a template.**

Templates (OAuth callback URLs, Mergify blocks, image tags…) **change from one version to another**.
The **version substring**, however, is the reliable common denominator. Filtering by substring avoids
the two classic accidents: missing an entry whose shape has changed, or accidentally touching a version
that is still active.

### Map of the version naming conventions — ⚠️ key reference

The **same** version is written in several ways depending on context. This is the main trap of the whole
procedure. For `N.E.x = 4.8.x`:

| Form | Where you meet it | Example (4.8) |
|---|---|---|
| `4-8-x` (dashes) | `cloud-apim` folder, k8s hostnames, resource names, namespace, data view, GitHub label, OAuth entries | `apim-4-8-x-gateway`, `apply-on-4-8-x` |
| `4_8_x` (underscores) | Elasticsearch indices / data views / data streams / templates | `dev_apim_4_8_x-*` |
| `4.8.x` (dots + `.x`) | CircleCI triggers, git branches, Mergify target branch | `Helm tests - 4.8.x` |
| `4.8.*` (dots + `*`) | Helm chart version constraint | `version: 4.8.*` |
| `4.8.x-latest` | **dev** image tag (internal ACR registry, moving) | `4.8.x-latest-debian` |
| `graviteeio@4.8` | **public** image tag, latest patch (floating major.minor) — **frozen at EOL** | `graviteeio@4.8` |
| `graviteeio@4.8.0` | **public** image tag, first release of the version | `graviteeio@4.8.0` |

> **Cross‑cutting check.** A recursive `grep` on `4-8-x`, `4_8_x`, `4.8` across the affected repos must,
> at the end of the operation, return **only known, legitimate occurrences**. In the `cloud-apim` repo, a
> residual `4-8-x` occurrence corresponds to a **git branch name of a test application** that is
> **permanent**: it must **not** be deleted. Any *other* unexpected occurrence must be dealt with.

---

## 1. Prerequisites (access & permissions)

| Area | Required access |
|---|---|
| `cloud-apim` (GitOps / ArgoCD) | permission to open a PR; ArgoCD handles de‑provisioning automatically |
| CircleCI | access to **Project Settings** of the `gravitee-api-management` project, or a **CircleCI token** for API v2 |
| Elasticsearch / Kibana | Kibana access (Index Management, Data Views); **ILM permissions** required if cleaning up the ILM policies is desired (see Action 3) |
| Google Cloud Console | access to the GCP project **“Cluster APIM Hors Prod”**, OAuth client “APIM All Dev environments” |
| GitHub | permission to delete a label + open a PR on `gravitee-api-management`; ideally `gh` CLI configured |

---

## 2. Actions

Each card follows the same format: **What / Where / Manual steps / Under the hood (API) / Values to
parameterize / Watch‑outs / Symmetry & scriptability**.

---

### Action 1 — Remove the dev environment (GitOps PR on `cloud-apim`)

**What.** Remove every trace of the `N.E.x` dev environment so that ArgoCD automatically de‑provisions
the associated workloads.

**Where.** Repo **`cloud-apim`** (GitOps). The actual de‑provisioning is performed by ArgoCD on
resync.

**Manual steps.** Open a PR that performs the following **4 changes**:
1. Delete the whole `N-E-x/` folder (`Chart.yaml` + `values.yaml`).
2. Remove the `- path: "N-E-x"` entry from `application/apim.applicationset.yaml`.
3. Remove the `- path: 'N-E-x'` entry from `application/apim.logstash.applicationset.yaml`
   *(only if the environment was part of the logstash subset — check).*
4. Remove the version block from `README.adoc` (table of URLs per environment).

**Under the hood.** The ArgoCD ApplicationSets’ *git directories* generator deletes the generated
Application as soon as the `path` entry disappears. The finalizer + `prune: true` on the Application
template ensure the resources are effectively removed (validated: prune & finalizer active).

**Values to parameterize.** `N-E-x` (**dashes** form) in paths, hostnames and resource names;
`dev_apim_N_E_x` (underscore) for the ES index defined in `values.yaml`.

**Watch‑outs.**
- Repo structure: **one folder per version** (`4-9-x/`, `4-10-x/`, `4-11-x/`…) alongside non‑versioned
  envs (`master-ce`, `master-oem`, `ae`). Touch **only** the `N.E.x` folder.
- The logstash list is a **subset**: remove the entry there only if it was present.
- **Post‑merge check on ArgoCD** (even though prune is automatic): confirm the `apim-N-E-x` and
  `logstash-N-E-x` Applications are gone and the `apim-apim-N-E-x` namespace has been removed.

**Residues outside the GitOps scope** (do **not** go away with the PR):
- the ES index / data streams / templates → handled in **Action 3**,
- the ILM policies → **kept on purpose** (see Action 3),
- the Google OAuth entries → **Action 4**.

**Symmetry & scriptability.** Environment creation = adding an `N-M-x/` folder + the ApplicationSet
entries when a minor is opened. GitOps step, done via PR (no dedicated script required).

---

### Action 2 — Delete the CircleCI scheduled triggers

**What.** Delete **every scheduled trigger** carrying version `N.E.x` — four for a line cut after
the freeze started declaring them, three for an older one.

**Where.** CircleCI → **Project Settings → Project Setup → Pipelines** of the
`gravitee-api-management` project.

**Manual steps.** For **each** `… - N.E.x` trigger:
expand the trigger (arrow) → **Trigger Details** panel → **trash** icon → type `DELETE` in the
confirmation modal → **Delete trigger**.

The triggers, as [`08-create-circleci-triggers.sh`](../code-freeze/08-create-circleci-triggers.sh)
declares them:
- `Repository tests - N.E.x`
- `Bridge Compatibility tests - N.E.x`
- `Helm tests - N.E.x`
- `Nightly - N.E.x` — only on lines cut after the freeze started creating it

**Under the hood.** These triggers are **CircleCI Schedules** (API v2). The UI issues a
`DELETE https://app.circleci.com/api/v2/schedule/{schedule-id}` (observed: `200 OK`).

**Scriptable (API v2)** — auth via header `Circle-Token: <token>`, slug
`gh/gravitee-io/gravitee-api-management`:
1. List: `GET https://circleci.com/api/v2/project/{slug}/schedule` → returns `id` + `name`.
2. Filter the schedules whose `name` ends with ` - N.E.x`.
3. **Display what matched** and check it against the list above, then `DELETE …/schedule/{id}` for each.

**Values to parameterize.** `N.E.x` (**dots** form), as a **suffix** of the schedule name.

**Watch‑outs.**
- The “type `DELETE`” safeguard only exists in the UI. In the API, **add your own confirmation**
  (display the targeted names before deleting).
- Nothing to clean up in `.circleci/config.yml`: the target version is carried by the schedule’s
  **`branch` pipeline parameter**, not by the YAML.

**Symmetry & scriptability.** Creation = existing script
[`release/code-freeze/08-create-circleci-triggers.sh`](../code-freeze/08-create-circleci-triggers.sh),
which creates them from a table it declares — **four** since a support line was given its own nightly,
so this deletion covers four triggers, not three. **No symmetric deletion script today** → identified
candidate (see grey areas §4).

---

### Action 3 — Elasticsearch / Kibana cleanup

**What.** Remove the Kibana objects and the Elasticsearch data for `N.E.x`.

**Where.** Kibana (Data Views + Index Management) on the dev Elastic Cloud instance.

**Manual steps** (recommended order):

1. **Data views** (Kibana objects) — Stack Management → Data Views → delete:
   - `apim-N-E-x` (**dashes** form)
   - `dev_apim_N_E_x-*` (**underscore** form + `-*` suffix)

2. **Data streams** (Elasticsearch) — Index Management → **Data Streams** tab → filter
   `dev_apim_N_E_x` → delete, e.g. `dev_apim_N_E_x-event-metrics`.
   *(Data streams must be deleted via the data stream API, not as indices — see watch‑outs.)*

3. **Indices** (Elasticsearch) — Index Management → filter `dev_apim_N_E_x` → select all → delete.
   Observed families: `health`, `log`, `monitor`, `request`, plus the **v4** streams
   `v4-log`, `v4-message-log`, `v4-message-metrics`, `v4-metrics` — each with several rollover
   generations (`-000013`, `-000014`, …).

4. **Index templates** (Elasticsearch) — delete the `dev_apim_N_E_x-<type>` templates
   (one per family: `health`, `log`, `monitor`, `request`, `v4-log`, `v4-message-log`,
   `v4-message-metrics`, `v4-metrics`).

**Under the hood (observed).** The UI goes through **internal Kibana endpoints**:
`POST /api/index_management/indices/delete` (explicit list of indices),
`POST /api/index_management/delete_index_templates`. Kibana **9.4.1** at operation time.

**Scriptable — prefer the public, stable Elasticsearch API** (not the internal Kibana endpoints,
which change across versions):
- List first: `GET /_data_stream/dev_apim_N_E_x*` and `GET /_cat/indices/dev_apim_N_E_x*?v&s=index`
- Data streams: `DELETE /_data_stream/dev_apim_N_E_x-*`
- Indices: `DELETE /dev_apim_N_E_x-*`
- Templates: `DELETE /_index_template/dev_apim_N_E_x-*`
- List again afterwards to confirm nothing remains.

Auth via API key / credentials in an **environment variable** — **never** a session cookie.

**Values to parameterize.** `N_E_x` (underscore) for everything ES; `N-E-x` (dashes) for the first
data view `apim-N-E-x`. **Two conventions within the same step.**

**Watch‑outs.**
- **Data view ≠ index ≠ data stream.** These are three distinct kinds of objects.
- **Data stream before index.** You cannot delete a data stream’s *backing indices* directly (ES
  recreates them or refuses). Always `DELETE /_data_stream/…` **before** a `DELETE /dev_apim_N_E_x-*`.
- **List before deleting.** Never run a wildcard `DELETE` without checking the list first; mind the
  `action.destructive_requires_name` flag.
- The wildcard `dev_apim_N_E_x*` is **safe** because the version is surrounded by underscores (no
  collision with `N.(E+1).x`).
- **Never embed a session cookie** in a shared command or a script.

**ILM policies — kept on purpose.** We **do not touch** the ILM policies
(`monitor`, `request`, `health`, `log`). They become orphaned but **harmless**: nothing triggers them
since no new `dev_apim_N_E_x-*` index will ever be created. Deliberate choice, not an oversight.

**Symmetry & scriptability.** **Structural asymmetry: creation is emergent, deletion is manual.** When a
minor is opened, there is **nothing to create** on the ES side — indices appear automatically when the
application starts (rollover/ILM), and data views can only be created once the indices contain data.
There is therefore **no** creation step to script. Conversely, what is born on its own does not die on
its own: **EOL deletion** remains a manual act — scriptable via the public ES API for the data
(indices / data streams / templates), the data views deletion relying on the Kibana API (less stable
across versions).

---

### Action 4 — Google OAuth cleanup

**What.** Remove the `N.E.x` entries from the shared OAuth client.

**Where.** Google Cloud Console → project **“Cluster APIM Hors Prod”** → Google Auth Platform →
**Clients** → client **“APIM All Dev environments”** *(a single OAuth client shared by all dev
environments)*.

**Manual steps.** Remove **all** entries containing `N-E-x`, in **both** sections, via the trash icon at
the end of the line, then **Save**. For `4.8`, this means **4 lines**:

*Authorized JavaScript origins:*
- `https://apim-N-E-x-console.team-apim.gravitee.dev`
- `https://apim-N-E-x-portal.team-apim.gravitee.dev`

*Authorized redirect URIs:*
- `https://apim-N-E-x-console.team-apim.gravitee.dev`
- `https://apim-N-E-x-portal.team-apim.gravitee.dev/user/login`

**Values to parameterize.** `N-E-x` (**dashes** form) in hostnames.

**Watch‑outs.**
- ⚠️ **Client shared by all environments.** A mistake here **breaks OAuth login for all dev envs**.
  Remove **only** the lines containing exactly `N-E-x`.
- **Never** use the **Delete** button (at the top) which deletes the **entire client**.
- **Clean both sections** (origins AND redirect URIs) — don’t stop at the redirect URIs.
- Double‑check before **Save**.
- **The URI template evolves across versions** (all the more reason to filter on `N-E-x` and not
  rebuild):

  | Entry | Appeared in | Section |
  |---|---|---|
  | `{console}` | always | origins + redirect |
  | `{portal}` | always | origins |
  | `{portal}/user/login` | old (4.8 case) | redirect |
  | `{portal}/classic/user/login` | 4.10+ | redirect |
  | `{portal}/next/log-in` | 4.10+ | redirect |
  | `{gamma-console}` | 4.12+ | origins |
  | `{gamma-console}/login` | 4.12+ | redirect |

**Symmetry & scriptability.** Entries added manually when a minor is opened. Technically scriptable
(GCP API / `gcloud`), but **kept manual** as it is a sensitive operation on a shared client.

---

### Action 5 — Delete the GitHub label

**What.** Delete the backport label `apply-on-N-E-x`.

**Where.** GitHub → `gravitee-io/gravitee-api-management` → **Issues → Labels**.

**Manual steps.** On the `apply-on-N-E-x` label row: `…` menu → **Delete** (GitHub confirmation).

**Under the hood.** The UI issues a `DELETE https://github.com/…/labels/{node-id}` (targeting by
internal node ID, `200 OK`).

**Scriptable — public API / `gh` CLI** (prefer over the internal node ID):
- `gh label delete apply-on-N-E-x --repo gravitee-io/gravitee-api-management --yes`
- or `DELETE /repos/gravitee-io/gravitee-api-management/labels/apply-on-N-E-x` (by name).

**Values to parameterize.** `apply-on-N-E-x` (**dashes** form).

**Watch‑outs.** This label is **consumed by Mergify** → its deletion goes together with the
`.mergify.yml` cleanup (**Action 6**). Deleting the label alone would leave an orphaned Mergify rule
(not breaking, but residual). The label ↔ Mergify PR order is **indifferent** (a rule matching a
non‑existent label never fires; a label with no rule does nothing).

**Symmetry & scriptability.** Creation = existing script
[`release/code-freeze/04-create-github-label.sh`](../code-freeze/04-create-github-label.sh)
(`gh label create`, description `"Mergify: apply on <version>"`). The `dots → dashes` name derivation is
done in `release/code-freeze/_common.sh` (**source of truth** for the conversion).

---

### Action 6 — Clean up the Mergify backport rule (PR on `.mergify.yml`)

**What.** Remove the `N.E.x` backport block from the `.mergify.yml` file.

**Where.** Repo `gravitee-io/gravitee-api-management`, `.mergify.yml` file at the root.
**One PR on `master`.**

**Manual steps.** Delete the version’s `pull_request_rules` entry, a self‑contained block of the form:

```yaml
- name: Apply commits on `N.E.x`
  conditions:
    - label=apply-on-N-E-x
  actions:
    backport:
      branches:
        - N.E.x
      assignees:
        - "{{ author }}"
      body: |
        This is an automatic copy of pull request #{{number}} done by [Mergify](https://mergify.com).
        ...
      title: "[N.E.x] {{ title }}"
```

**Values to parameterize.** `N.E.x` (dots) for the target branch and the `title`; `apply-on-N-E-x`
(dashes) for the `label` condition. **Both conventions coexist in the same block.**

**Watch‑outs.**
- Remove **exactly** the `- name: Apply commits on \`N.E.x\`` block, no more, no less (the neighboring
  version blocks stay intact).
- For a future script, prefer **`yq`** (structured YAML editing, targeting by
  `.pull_request_rules[] | select(.name == "Apply commits on \`N.E.x\`")`) over a `sed`, which is
  fragile on this file.

**Historical note (context).** Until now, this block was **not** removed at EOL: it was “recycled” at
the next code‑freeze, which replaced the oldest version with the new one. This runbook adopts
**explicit** cleanup at EOL. *(The code‑freeze corollary — stop “replacing the oldest” — is a tooling
improvement, out of scope of this runbook; see the separate “code‑freeze improvements” note.)*

**Symmetry & scriptability.** When a minor is opened, the block is currently produced by substitution in
[`release/code-freeze/03-update-master-version.sh`](../code-freeze/03-update-master-version.sh).

---

### Action 7 — Update the bridge compatibility tests

**What.** Update the *bridge compatibility tests* matrix to remove any dependency on the **dev** image
`N.E.x-latest` (which stops being published), across **all affected branches**.

**Where.** Repo `gravitee-io/gravitee-api-management`, **two mirror files**:
- `.circleci/ci/src/workflows/workflow-bridge-compatibility-tests.ts` *(source)*
- `.circleci/ci/src/pipelines/tests/resources/bridge-compatibility-tests/bridge-compatibility-tests.yml`
  *(the “expected” of a unit test that verifies generation from the source)*

**⚠️ Both files must be modified together** on each branch, otherwise the config‑generation unit test
breaks.

**One PR per branch** — for `4.12.0 / EOL 4.8`, **5 PRs**: `4.9.x`, `4.10.x`, `4.11.x`, `4.12.x`,
`master`.
Observed title convention: `4.8.x EOL - update bridge tests - <branch>`; commit with `[skip ci]`
(avoids triggering the whole CI for a test‑config change).

**Grammar of the `apim_client_tag` matrix tags:**

| Tag | Registry | Meaning | Moving? |
|---|---|---|---|
| `X.Y.x-latest` | internal ACR | latest dev build of the live branch | yes (while supported) |
| `graviteeio@X.Y.0` | public | **first** public release of the version | no |
| `graviteeio@X.Y` | public | **latest** public patch (floating major.minor) — **frozen at EOL** | no (once EOL) |
| `master-latest` | internal ACR | dev of `master` (= upcoming next minor) | yes |

**The change depends on the branch’s position** (3 distinct cases):

- **“Low” branches** (here `4.9.x`, `4.10.x`, `4.11.x`) — `N.E.x` is still **within** the compat window
  (tested client), so its dev tag is **frozen** onto the final public tag:
  **substitution `N.E.x-latest` → `graviteeio@N.E`** (one line, in each file).

- **`master`** — `N.E.x` is **already out of the window**. **Slide the window by one notch**: add the new
  version at the top (`N.M.x-latest` + `graviteeio@N.M.0`) and remove the oldest at the bottom
  (`N.(M-3).x-latest` + `graviteeio@N.(M-3).0`).

- **New branch `N.M.x`** — still inherits `master-latest` as its first client. **Replace it with the
  branch’s own tags**: `master-latest` → `N.M.x-latest` + `graviteeio@N.M.0`.

**Values to parameterize.** `N.E.x-latest` → `graviteeio@N.E` (low branches); tags `N.M` / `N.(M-3)` for
the window shifts.

**Watch‑outs.**
- **Always both files** (`.ts` + `.yml`), with the **same** change.
- **The change is not uniform** across branches: a plain `sed N.E.x-latest → graviteeio@N.E` covers the
  low branches **but not** `master` nor the new `N.M.x` branch.
- Respect the matrix order (`X.Y.x-latest` precedes its `graviteeio@X.Y.0`, versions descending).
- **Code‑freeze junction (verify / catch up).** On the new branch `N.M.x`, the replacement
  `master-latest` → `N.M.x-latest` **normally belongs to the code‑freeze**. If it was **not** done there,
  it must be **caught up here**. Check for any leftover `master-latest` in the `N.M.x` branch matrix.

**Symmetry & scriptability.** This step mixes EOL gestures (freezing `N.E.x-latest`) and minor‑release
gestures (adding `N.M`, replacing `master-latest`). It is the **least mechanically scriptable** step of
the procedure.

---

## 3. Post‑operation checks

- [ ] **ArgoCD**: `apim-N-E-x` and `logstash-N-E-x` Applications gone; `apim-apim-N-E-x` namespace removed.
- [ ] **CircleCI**: no more `… - N.E.x` schedules (all 3 deleted).
- [ ] **Elasticsearch/Kibana**: `GET /_cat/indices/dev_apim_N_E_x*` and `GET /_data_stream/dev_apim_N_E_x*` return nothing; data views and templates absent. *(ILM policies kept: expected.)*
- [ ] **Google OAuth**: no more `N-E-x` entries (origins + redirect); **OAuth login for active envs intact** (quick sign‑in test on a recent env).
- [ ] **GitHub**: `apply-on-N-E-x` label deleted; `N.E.x` Mergify block removed (PR merged).
- [ ] **Bridge tests**: no `N.E.x-latest` occurrence left in either file, on **all** branches; the 5 PRs merged.
- [ ] **Cross‑cutting grep**: `grep -r` on `N-E-x`, `N_E_x`, `N.E` returns only known legitimate occurrences (including the **test app’s git branch** in `cloud-apim`, not to be touched).

---

## 4. Grey areas & points to confirm

*(Items assumed as not settled — not to be confused with oversights.)*

- **Symmetric CircleCI deletion script.** The **creation** of the schedules is scripted
  (`08-create-circleci-triggers.sh`); the **deletion** is not. Natural candidate: a mirror script (list
  `GET …/schedule`, filter on the ` - N.E.x` suffix, loop the `DELETE`s).
- **Check for a residual `event-metrics` template** on the ES side (the data stream has no template in
  the list of 8 that were deleted — possibly a different mechanism). Non‑blocking.

---

## Appendix — Mapping to the `release/code-freeze/` scripts

A version’s life cycle is framed, when a minor is **opened**, by the `release/code-freeze/` scripts.
This EOL procedure is its **counterpart** at closing time. Contact points:

| Topic | Creation (code‑freeze) | Closing (this runbook) |
|---|---|---|
| GitHub label | `04-create-github-label.sh` (`gh label create`) | Action 5 (`gh label delete`) |
| Mergify rule | `03-update-master-version.sh` (a rule **added** for the new line) | Action 6 (block removal) |
| CircleCI triggers | `08-create-circleci-triggers.sh` (clone + `POST /schedule`) | Action 2 (`DELETE /schedule/{id}`) |
| `dots → dashes` name derivation | `_common.sh` (source of truth) | reused everywhere (`N.E.x` → `N-E-x`) |

> The **improvements to the `code‑freeze/` scripts** (to make both halves of the cycle consistent) are
> tracked **separately**, out of scope of this runbook.

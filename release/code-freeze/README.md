# Runbook — Code freeze

Opening a minor: cutting `<major>.<minor>.x` off master, moving master on to the next minor, and
giving the new line everything it needs to build, deploy and release on its own.

`release/code-freeze.sh` runs the nine steps below in order; each is also runnable on its own, and
the entry point takes a step number to resume from (`./release/code-freeze.sh 5`).

This file is the one place the freeze is written down. Its counterpart at the other end of a
version's life is [`../end-of-life/README.md`](../end-of-life/README.md).

## What the freeze produces

| | |
|---|---|
| `<major>.<minor>.x` | the support branch, at `<revision>-alpha.1-SNAPSHOT` |
| `master` | moved on to `<major>.<minor+1>.0-SNAPSHOT` |
| `apply-on-<major>-<minor>-x` | the GitHub label Mergify backports on |
| `apim-<revision>-alpha.1.tgz` | the Helm chart, on the private OCI registry |
| `<major>-<minor>-x` | the dev environment, through a pull request on `cloud-apim` |
| 3 CircleCI schedules | Helm tests, Bridge Compatibility tests, Repository tests — the nightly is missing, see below |

Everything is derived from **the root pom's `<revision>`**, read by `_common.sh`. Nothing is passed
as an argument, so the freeze releases whatever master currently says it is.

## Prerequisites

- `git`, `gh`, `az`, `helm`, `jq`, `curl` on the PATH — `_common.sh` refuses to go further without them.
- `release/.env` carrying `CIRCLECI_TOKEN`, for step 08.
- Docker running, for the Helm push through Azure ACR.
- `cloud-apim` cloned **beside** `gravitee-api-management`; step 00 clones it if it is missing.
- Admin rights on the repository: step 03 pushes master directly, and master is protected.

## The steps

| | Script | What it does | Where |
|---|---|---|---|
| 00 | `00-check-repos.sh` | Clones `cloud-apim` if absent | local |
| 01 | `01-create-branch.sh` | Cuts `<major>.<minor>.x` off an up-to-date master | local |
| 02 | `02-update-branch-version.sh` | Sets `-alpha.1` on both poms, the portal OpenAPI and the chart; commits; pushes the branch | branch |
| 03 | `03-update-master-version.sh` | Bumps `<revision>` to the next minor on both poms, the portal OpenAPI and the chart; empties the chart's `artifacthub.io/changes`; swaps the oldest branch out of `.mergify.yml`; pushes master | master |
| 04 | `04-create-github-label.sh` | `gh label create apply-on-<major>-<minor>-x` | GitHub |
| 05 | `05-publish-helm-charts.sh` | `helm package` then `helm push` to `oci://graviteeio.azurecr.io/helm/` | Azure ACR |
| 06 | `06-create-cloud-apim-env.sh` | Copies the previous environment, wires it into the three applicationsets, opens the pull request | cloud-apim |
| 07 | `07-update-google-oauth.sh` | **Manual.** Prints the origins and redirect URIs to add, then waits on Enter | Google Cloud Console |
| 08 | `08-create-circleci-triggers.sh` | Clones the oldest schedule of each of three names onto the new branch | CircleCI |

Then, from the script's own closing summary, three things nobody has scripted: update the plugins
that depend on the APIM BOM, release each library and plugin from alpha to its official version, and
update those versions in the APIM pom.

## What the freeze does not do

Each of these is a gesture someone has to remember. They are listed with what happens when nobody
does, because none of them fails loudly.

### The core pin is never moved

Measured: `grep -rn "apim.core.version" release/code-freeze/` returns nothing but a comment. The only
writer of that property anywhere is the pinning job of the core release lane.

Each branch should pin the core it publishes itself: `<major>.<minor>.0-alpha.1-SNAPSHOT` on the new
branch, `<major>.<minor+1>.0-SNAPSHOT` on master. Note that the pin is a whole coordinate, not the
`<revision>` half of one — written `4.14.0` it would name a release nobody has published.

That was harmless while the pin was inert. Since BX-393 the pin decides which core gets assembled,
and master pins its own version as a `-SNAPSHOT` — which is what keeps master structurally
unreleasable. Three things follow from nobody moving it:

1. **The new branch cannot release.** `<major>.<minor>.x` inherits `<major>.<minor>.0-SNAPSHOT` as
   its pin, and `assertPinIsReleasable` refuses a SNAPSHOT before the pipeline is even triggered —
   the publishing lane refuses it again, through Maven, before anything is built. The branch's first
   act therefore has to be a core release followed by merging the pinning pull request. The refusal
   is loud and immediate; what nobody says is what to do about it.
2. **Master's pin goes stale.** Step 03 moves `<revision>` to the next minor and leaves the pin
   where it was, so distribution-only pull requests on master — which assemble the pin, not the
   branch's core — build against a version master no longer publishes.
3. **That version becomes a dead coordinate.** Master publishes `<major>.<minor+1>.0-SNAPSHOT`, the
   branch publishes `<major>.<minor>.0-alpha.1-SNAPSHOT`, and nothing republishes the pinned one.
   Both branches then rely on Nexus never purging a snapshot nobody produces any more.

### Three schedules created, four needed

A support branch runs four scheduled pipelines: Helm tests, Repository tests, Bridge Compatibility
tests and the **nightly**. Step 08 creates the first three. The two remaining scheduled actions
`config.yml` declares — `integration_tests` and `run_e2e_tests` — are triggered on demand, not
scheduled; the nightly already carries the e2e jobs (`workflow-nightly.ts`).

So a freshly cut branch has no nightly until someone notices, and nothing makes them notice: a
missing schedule turns no job red, the pipeline simply never runs.

Two smaller things in the same script: it clones **the oldest** schedule of each name, so the new
branch inherits the parameters of the least recently touched line; and re-running it creates
duplicates rather than recognising what it already made.

### The bridge compatibility matrix is not adapted

A server is tested against **itself and the three previous minors**. The matrix is a literal list in
`workflow-bridge-compatibility-tests.ts`, and each branch carries its own copy of it, so a freeze has
two of them to adapt:

| | on `<major>.<minor>.x` | on master |
|---|---|---|
| itself | `master-latest` becomes `<major>.<minor>.x-latest` | stays `master-latest` |
| the new line | — | add `<major>.<minor>.x-latest` |
| the line that falls out | — | drop the oldest `…-latest` and `graviteeio@…` pair |

The branch's copy therefore needs one substitution, master's a shift. `graviteeio@<major>.<minor>.0`
joins master's list only when that version is actually released — at the freeze it does not exist
yet.

Creating the schedule, which step 08 does, is not enough on its own: the job would run against a
matrix that ignores the line just opened.

### One end-of-life gesture, done too early

A line leaves support when the **first release of the new minor ships**, not when its branch is cut —
opening 4.13 retires 4.9, once 4.13.0 is out. Step 03 does not wait: it substitutes the oldest branch
in `.mergify.yml` for the new one, and its own sort names `4.9.x` today.

From the day the branch is cut, then, Mergify no longer backports to a line that is still supported
for the whole alpha phase, and a fix meant for it has to be carried by hand — with nothing to say so.
The gesture belongs to [the end-of-life runbook](../end-of-life/README.md), which the first release
of the new minor triggers.

### No deletion counterpart

Creating the schedules is scripted, deleting them is not — the end-of-life runbook says the same and
leaves the mirror script to this ticket.

### Two substitutions that can quietly match nothing

Both poms are edited with the root pom's values:

- step 02 matches `<sha1 />` alone, so a pom already carrying the empty non-self-closing form is
  skipped;
- step 03 substitutes `<revision>${REVISION}</revision>` with the revision read from the **root**
  pom, so a distribution pom on another revision is skipped.

Today both poms carry `<revision>4.13.0</revision>` and `<sha1 />`, so both substitutions match.
They are listed because the whole point of the two reactors is that those numbers stop agreeing, and
a `sed` that matches nothing exits 0.

### Nothing checks the result

No step re-reads what the previous ones wrote. The closing summary prints what the scripts *meant*
to do, computed from the same variables they used, not from the repository.

## Checks after the freeze

- [ ] `<major>.<minor>.x` exists on the remote, at `<revision>-alpha.1-SNAPSHOT` in **both** poms.
- [ ] Master builds at `<major>.<minor+1>.0-SNAPSHOT` in **both** poms.
- [ ] The distribution pom on the new branch pins a **released** core, not a SNAPSHOT.
- [ ] Master's pin names a version master still publishes.
- [ ] `.mergify.yml` names the new branch, and **still names the line that has not been retired yet**.
- [ ] The bridge compatibility matrix names the new line, on the branch and on master.
- [ ] The `apply-on-<major>-<minor>-x` label exists.
- [ ] The chart is on the OCI registry under `apim-<revision>-alpha.1.tgz`.
- [ ] The `cloud-apim` pull request is open, and merged once reviewed.
- [ ] The Google OAuth client carries the four redirect URIs and two origins of the new environment.
- [ ] The new branch's four schedules exist — nightly included — and none was duplicated.
- [ ] A release from the new branch is possible — `yarn prepare_distribution_release --version=… --dry-run`
      passes its preconditions.

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
| 4 CircleCI schedules | Helm tests, Bridge Compatibility tests, Repository tests, and the nightly |

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
| 02 | `02-update-branch-version.sh` | Sets `-alpha.1` on both poms, **pins the core the branch will publish**, updates the portal OpenAPI and the chart; commits; pushes the branch | branch |
| 03 | `03-update-master-version.sh` | Bumps `<revision>` to the next minor on both poms, **moves the pin with it**, updates the portal OpenAPI and the chart; empties the chart's `artifacthub.io/changes`; adds the new line's Mergify backport rule; pushes master | master |
| 04 | `04-create-github-label.sh` | `gh label create apply-on-<major>-<minor>-x` | GitHub |
| 05 | `05-publish-helm-charts.sh` | `helm package` then `helm push` to `oci://graviteeio.azurecr.io/helm/` | Azure ACR |
| 06 | `06-create-cloud-apim-env.sh` | Copies the previous environment, wires it into the three applicationsets, opens the pull request | cloud-apim |
| 07 | `07-update-google-oauth.sh` | **Manual.** Prints the origins and redirect URIs to add, then waits on Enter | Google Cloud Console |
| 08 | `08-create-circleci-triggers.sh` | Creates the branch's four scheduled pipelines, from the table it declares | CircleCI |

Then, from the script's own closing summary, three things nobody has scripted: update the plugins
that depend on the APIM BOM, release each library and plugin from alpha to its official version, and
update those versions in the APIM pom.

## What the freeze does not do

Each of these is a gesture someone has to remember. They are listed with what happens when nobody
does, because none of them fails loudly.

### The pin moves, but a fresh branch still cannot release

Each branch pins the core it publishes itself — `set_core_pin` in `_common.sh` writes it and reads it
back, because a `sed` that matches nothing exits 0. So a freeze leaves the new branch on
`<major>.<minor>.0-alpha.1-SNAPSHOT` and master on `<major>.<minor+1>.0-SNAPSHOT`, and neither
assembles a core the other publishes.

What that does not do is make the new line releasable. A release refuses a SNAPSHOT pin — locally,
through `assertPinIsReleasable`, before the pipeline is even triggered, and again in the publishing
lane through Maven. **The branch's first act therefore has to be a core release, followed by merging
the pinning pull request the core lane opens on it.** The refusal is loud and immediate; what nobody
says is what to do about it.

### The schedules are declared, not copied

Until now a support line carried three scheduled pipelines and the nightly ran on master alone, so
every supported version went without the suites the nightly carries — the e2e ones among them. The
freeze now creates four, from a table at the top of step 08:

| Schedule | Action | Base hour (UTC) | Days |
|---|---|---|---|
| Bridge Compatibility tests | `bridge_compatibility_tests` | 02 | MON |
| Repository tests | `repositories_tests` | 03 | MON–FRI |
| Helm tests | `helm_tests` | 21 | WED, SUN |
| Nightly | `nightly` | 23 | MON–FRI |

Those are 4.12.x's hours, and each line shifts them by an offset of its own — two hours per step,
cycling over the four minors supported at a time. So `4.13.x` runs its bridge suite at 04 rather
than at 02, and no two supported lines start the same suite together.

**Master sits one hour after the base**, on every suite: bridge at 03 against 02, repository at 04
against 03, helm at 22 against 21, the nightly at 00 against 23. Taking only even offsets is what
keeps a line off master's hour, so the two never start the same suite at once. Worth preserving if
these hours are ever changed.

The step used to clone the oldest schedule of each name, which meant a new branch inherited whatever
the least recently touched line happened to carry, and nothing in the repository said what that was.
Declared, the hours are reviewable and the step works on a project that has no schedules at all.

Every schedule the step creates is named `<Suite> - <branch>`, and its description says the same in
the same words. One of master's predates that convention and reads `bridge_compatibility_tests_master`;
it is left where it is — nothing depends on a schedule's name — but nothing here reproduces it, so a
listing sorted by name keeps the lines together.

### The bridge compatibility matrix takes care of itself

A server keeps talking to four client lines — its own and the three before it — and each line is
tested on two clients: its first release, and its last. What "last" means depends on the line: a
supported one still moves, so its tip is `<line>.x-latest` on our registry; a retired one stopped, so
its last is the public `graviteeio@<line>` tag. A line retires when the fourth release after it
ships, which makes the supported set the four most recent lines to have published a `.0`.

That list used to be written out by hand, one copy per branch: two files to edit at every freeze, and
five more every time a line retired. It could not be scripted either — the linter reflows the array as
soon as its length changes, so the next freeze's substitutions would find nothing to match.

`bridgeClientTags` derives it from the version in the tree and the tags the repository carries. **A
freeze has nothing to do here, neither has the release that follows it, nor the retirement it
triggers.** The derivation is checked against the four lists master and the support branches carry
today — reproduce them, or the rule is wrong.

The one case it refuses to guess is a line with fewer than three previous minors in its own major:
which versions of the previous major a `5.0` should keep talking to is not something a version number
answers. It stops the config generation and says so.

### Mergify gains a line here, and loses one elsewhere

A line leaves support when the **first release of the new minor ships**, not when its branch is cut:
opening 4.13 retires 4.9, once 4.13.0 is out. The freeze used to substitute the oldest branch for the
new one in `.mergify.yml`, which retired it weeks early — a fix meant for it then had no backport rule
and had to be carried by hand, with nothing to say so.

The freeze now only adds, so the file carries five lines for as long as the alpha lasts. Removing the
one that retires belongs to [the end-of-life runbook](../end-of-life/README.md), which the first
release of the new minor triggers.

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
a `sed` that matches nothing exits 0. The pin is the one write that reads itself back; the others
still do not.

### Nothing checks the result

Apart from the pin, no step re-reads what it wrote. The closing summary prints what the scripts
*meant* to do, computed from the same variables they used, not from the repository.

## Checks after the freeze

- [ ] `<major>.<minor>.x` exists on the remote, at `<revision>-alpha.1-SNAPSHOT` in **both** poms.
- [ ] Master builds at `<major>.<minor+1>.0-SNAPSHOT` in **both** poms.
- [ ] The new branch pins `<revision>-alpha.1-SNAPSHOT`, and master pins `<major>.<minor+1>.0-SNAPSHOT`.
- [ ] Releasing the new line for real is understood to need a core release first, and its pinning
      pull request merged.
- [ ] `.mergify.yml` names the new branch, and **still names the line that has not been retired yet**.
- [ ] The bridge compatibility matrix names the new line, on the branch and on master.
- [ ] The `apply-on-<major>-<minor>-x` label exists.
- [ ] The chart is on the OCI registry under `apim-<revision>-alpha.1.tgz`.
- [ ] The `cloud-apim` pull request is open, and merged once reviewed.
- [ ] The Google OAuth client carries the four redirect URIs and two origins of the new environment.
- [ ] The new branch's four schedules exist, at the hours step 08 declares, and none is duplicated.
- [ ] A release from the new branch is possible — `yarn prepare_distribution_release --version=… --dry-run`
      passes its preconditions.

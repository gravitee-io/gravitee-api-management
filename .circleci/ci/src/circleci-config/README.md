# `circleci-config` — internal CircleCI config model

This module is an in-house, minimal replacement for
[`@circleci/circleci-config-sdk`](https://github.com/CircleCI-Public/circleci-config-sdk-ts),
which CircleCI **archived on 2 September 2025** (read-only, no longer maintained).

The SDK was only ever used here as a *typed YAML builder*. The dynamic side of the
CI — generating a pipeline that only builds what changed — lives entirely in our own
code (`utils/git.ts` change detection + the `shouldBuild*/shouldTest*` predicates,
run through CircleCI's native `setup` + `continuation` mechanism) and never depended
on the SDK. So only the YAML-emitting layer needed replacing.

## Design

The public surface (class names and namespaces) **mirrors the original SDK**, so the
~107 consumer files import from here without any other change:

```ts
import { Config, Job, Workflow, commands, reusable, workflow, executors, orb, parameters } from '../circleci-config';
```

Every component implements `Generable` and knows how to `generate()` the plain object
that maps to its CircleCI YAML fragment. `Config` collects the fragments (deduplicating
jobs/commands/orbs by name), orders the sections canonically, and serializes them with
the [`yaml`](https://www.npmjs.com/package/yaml) library.

| File | Contents |
|------|----------|
| `types.ts` | shared types (`Generable`, `Command`, resource classes) |
| `parameters.ts` | `CustomParameter`, `CustomEnumParameter`, `CustomParametersList` |
| `commands.ts` | native steps (`run`, `checkout`, cache, workspace, …) |
| `executors.ts` | `DockerExecutor`, `MachineExecutor` |
| `orb.ts` | `OrbImport`, `OrbRef` |
| `reusable.ts` | `ReusableCommand`, `ReusedCommand`, `ParameterizedJob` |
| `job.ts` / `workflow.ts` | `Job`, `Workflow`, `WorkflowJob` |
| `config.ts` | `Config` aggregator + `stringify()` |
| `index.ts` | barrel that mirrors the SDK surface |

## Serialization notes (kept byte-identical to the SDK)

- Native steps **spread their parameters**, so the caller's key order is preserved
  (e.g. `save_cache` emits `key`/`paths` in whatever order the call site passed them).
- `stringify()` uses `lineWidth: 0` (no wrapping) and `aliasDuplicateObjects: false`
  (a shared array such as the context list is inlined, not aliased with `&`/`*`).

## How it is validated

The generators are covered by golden-file tests in `src/pipelines/tests/`: each asserts
`generatedConfig.stringify()` equals a committed `.yml` fixture, byte-for-byte. The model
itself has unit tests under `tests/`.

## Extending the model

1. Add the class in the relevant file, implementing `generate()` to return the CircleCI
   fragment. Preserve caller key order by spreading the parameters where the SDK did.
2. Export it from `index.ts` (and from the matching namespace) if consumers reference it.
3. Add a unit test under `tests/`, and regenerate any affected fixture.

# `.ai/`: APIM's AI context

This directory is how APIM adopts [gravitee-ai-foundation](https://github.com/gravitee-io/gravitee-ai-foundation).
The hub authors shared rules and skills; this directory holds what is **specific to APIM**,
and the `gravitee-ai` CLI combines the two into the generated `AGENTS.md` files.

## What is here

- **`manifest.yaml`**: lists APIM's modules that get their own generated `AGENTS.md`, and is
  where the product (`apim`) is declared. On this branch the product line is intentionally
  left out, so the workshop exercise can add it. You edit this.
- **`rules/`**: APIM-local rules (`layer: local`), one topic per file. You edit these.
- **`generated.yaml`**: the ownership record: every hub-generated file and its hash. The CLI
  writes this; never edit it by hand (`gravitee-ai check` will fail if you do).

## Migration state (partial, by design)

APIM's old hand-written context (`.agent-rules/`, the root router `AGENTS.md`, and the
console module's `AGENTS.md`) has been migrated into `rules/`. Still hand-written and **not
yet migrated**: the other module `AGENTS.md` files (the backend Maven modules and the other
Angular apps). `setup` skips those loudly, which is expected until they are migrated too.

## Adding or changing a rule

1. **Check the hub first.** If the convention is not APIM-specific (general Java, Angular,
   security, git), it belongs in the hub, not here; open a hub PR instead, so every repo
   gets it.
2. **One topic per file**, `layer: local` frontmatter. Add `dirs:` to scope a rule to
   specific modules (see `apim-angular.md`); omit it for repo-wide rules.
3. **Rules earn their token cost.** Do not write down what an agent can infer from the code
   (scripts already in `package.json`, standard framework behaviour, basic syntax). The best
   rules come from mistakes an agent actually made in this repo repeatedly.
4. Run `gravitee-ai setup --repo .` to regenerate, then `gravitee-ai check --repo .` to
   confirm the generated files match. Review the `AGENTS.md` diff before committing.

## Commands

```bash
gravitee-ai detect --repo .    # what tech / archetype / product is detected
gravitee-ai setup  --repo .    # regenerate the AI context from hub + these sources
gravitee-ai check  --repo .    # verify generated files were not hand-edited
```

The CLI locates the hub from `GRAVITEE_AI_HOME`, a `--hub` flag, or the current directory; set the env var (workshop Part 0) so commands work from anywhere.

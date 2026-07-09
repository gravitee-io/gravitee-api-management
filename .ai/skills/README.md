# `.ai/skills/`: per-repo skill directives

Skills themselves are authored in [gravitee-ai-foundation](https://github.com/gravitee-io/gravitee-ai-foundation)
and distributed by `gravitee-ai setup` into `.claude/skills/` and `.agents/skills/`. This
directory does **not** define skills. It holds optional, repo-specific guidance that a hub
skill reads at runtime, so a general skill behaves like an APIM specialist.

## How it works (the mechanism)

- Three skills look for a directives file when they run: `explain-feature`,
  `analyze-test-coverage`, and `write-tests`. Each checks `.ai/skills/<slug>/directives.md`.
- If the file exists, the skill reads it and follows the guidance. If not, the skill detects
  conventions from the repository itself.
- The `gravitee-ai` CLI does not generate, track, or validate these files. They are
  hand-authored and committed by the team, and `gravitee-ai check` ignores them.
- Each hub skill ships a `directives-template.md` next to it (under `.claude/skills/<slug>/`)
  as a starting point; copy it here as `directives.md` and fill it in.

## The design principle: cost vs. reach

Two layers carry repo knowledge, and they trade off against each other:

- **Rules** (`.ai/rules/`, assembled into `AGENTS.md`) load on **every** model call. They are
  always in context, so every token is paid on every call. Keep them lean: fundamentals, plus
  a short pointer to where deeper detail lives.
- **Directives** (this directory) load **only when the matching skill runs**. This is the home
  for sizable, task-specific depth (detailed patterns, exemplars to copy, forbidden-pattern
  checklists) that would tax every unrelated task if it sat in a rule.

The rule of thumb: if the knowledge helps *any* task, it is a rule, and it should be short. If
it only matters while performing *one specific skill*, it is a directive. Content that sits in
between (useful sometimes, sizable) belongs on the lazy side: a lean rule that names it, and
the depth loaded on demand. `apim-testing.md` is an example: the always-on rule says where
each kind of test lives (useful for any test task) and points at the write-tests directives for
the detailed patterns (only loaded when writing tests).

## Best practice

- **Do not duplicate the rules.** A directive should not restate what the always-on rules
  already say (test naming, real-objects-over-mocks). Add only the skill-specific delta the
  agent cannot cheaply derive: scope shortcuts, exemplar file paths, prioritization judgment,
  forbidden-pattern checklists.
- **Point, do not copy.** Where a rule and a directive share content, keep it in one place and
  reference it from the other.
- **Keep it current.** Exemplar paths and shortcuts drift as the repo moves; a stale directive
  is worse than none. Review when the files it names move.

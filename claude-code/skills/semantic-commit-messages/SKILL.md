---
name: semantic-commit-messages
description: Write semantic commit messages following Gravitee changelog conventions
---

# Semantic Commit Messages for Gravitee

This skill provides guidelines for writing commit messages that integrate with Semantic Release and produce clean, user-facing changelogs.

## Why This Matters

Commit messages directly become changelog entries visible to users. Messages must be clear, relevant, and free of internal jargon.

## Release Triggers

| Commit Type                             | Triggers Release?   |
| --------------------------------------- | ------------------- |
| `feat:`                                 | Yes (minor bump)    |
| `fix:`                                  | Yes (patch bump)    |
| Any type with `BREAKING CHANGE` in body | Yes (major bump)    |
| `chore:`, `refactor:`, `test:`, `docs:` | No release          |

> [!WARNING]
> Spelling matters: `BREAKING CHANGE` triggers, `BREAKING CHANGES` does not.

## Commit Message Format

```
<type>(<optional-scope>): <user-facing description>
```

> [!IMPORTANT]
> **Single line only** — no multi-line bodies or bullet lists. Keep descriptions concise (~50 chars ideal, max 72).

## Rules

### DO

- Write **user-facing descriptions** that make sense in a changelog
- Use meaningful scopes that add context (e.g., `security`, `parser`, `runner`)
- Focus on **what changed** from the user's perspective
- Put JIRA tickets in the **footer**, never the first line

### DO NOT

- Include issue/ticket references in the first line — this means **JIRA tickets** (e.g., `GKIO-1234`), **GitHub issue numbers** (e.g., `(#42)`, `closes #42`), **PR numbers**, or any other tracker reference. Users cannot access these and they pollute changelogs. Link issues in PR bodies instead.
- Use useless generic scopes like `policy`, `project`
- Write internal-only descriptions like "address review feedback"
- Use generic messages like "fix bug" or "update code"

## Examples

### Good Commits (each line below is a separate, complete commit message)

```
feat(security): add API key and Bearer token credential support
fix(parser): handle null values in expression evaluation
feat(runner): support step-level parameter precedence over credentials
feat(api): remove deprecated securityHeaders API
refactor(auth): extract token validation into shared utility
```

### Bad Commits

```
chore: address copilot review comments       # Too generic
fix(policy): GKIO-1234 fix issue             # JIRA in first line
feat(runner): implement recovery step (#10)  # GitHub issue in first line
feat: update code                            # No meaningful description
```

## Skill Integration

When committing during GitHub skills, craft messages that describe the **actual change**:

| Instead of                               | Write                                                          |
| ---------------------------------------- | -------------------------------------------------------------- |
| `chore: address copilot review comments` | `fix(test): use correct assertion method for single-arg check` |
| `fix: fix bug`                           | `fix(runner): prevent duplicate Authorization headers`         |
| `chore: code review changes`             | `refactor(security): add case-insensitive header precedence`   |

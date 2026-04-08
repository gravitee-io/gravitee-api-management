# Global entrypoint for AI Agents for Coding Guidelines

This repository contains multiple backend modules in Java (Maven) and multiple frontend applications and libraries in Angular.

## Common rules

Shared coding standards live under [`.agent-rules/`](.agent-rules/).

> **Critical — always follow**
>
> **Only** read a common file when you are working in the **matching stack** (use the table below). If a common file is **already in your context** for this task, **do not read it again** (avoid duplicate rules and wasted tokens).

| Stack | When to read | File |
|-------|----------------|------|
| **Java** | Changes under a Maven module path listed under **Java** in *Nested `AGENTS.md` module roots* below | [`.agent-rules/java.md`](.agent-rules/java.md) |
| **Angular** | Changes under a path listed under **Angular** in *Nested `AGENTS.md` module roots* below | [`.agent-rules/angular.md`](.agent-rules/angular.md) |

## Nested `AGENTS.md` module roots

**Java** (top-level Maven modules; read `AGENTS.md` there when the file exists)

- `gravitee-apim-common/AGENTS.md`
- `gravitee-apim-definition/AGENTS.md`
- `gravitee-apim-distribution/AGENTS.md`
- `gravitee-apim-gateway/AGENTS.md`
- `gravitee-apim-integration-tests/AGENTS.md`
- `gravitee-apim-plugin/AGENTS.md`
- `gravitee-apim-repository/AGENTS.md`
- `gravitee-apim-reporter/AGENTS.md`
- `gravitee-apim-rest-api/AGENTS.md`

**Angular**

- `gravitee-apim-console-webui/AGENTS.md`
- `gravitee-apim-portal-webui-next/AGENTS.md`
- `gravitee-apim-webui-libs/gravitee-dashboard/AGENTS.md`
- `gravitee-apim-webui-libs/gravitee-kafka-explorer/AGENTS.md`
- `gravitee-apim-webui-libs/gravitee-markdown/AGENTS.md`


## Combining common and nested rules

1. Load **only** the common rule file(s) that match the code you are editing (Java and/or Angular — both if you touch both).
2. Find the **nested `AGENTS.md`** for that area (walk up from the files you edit, or open the module root from the list above).
3. **Apply common and nested rules together** — nested rules add or specialize; if something conflicts, prefer the **nested** guidance for that module.

## Contributing rules

- **Only add rules that earn their token cost.** Do not document things easily inferred from context (e.g., scripts already in `package.json`, standard framework behavior, basic language syntax).
- **The best rules come from observed agent mistakes.** If an agent repeatedly gets something wrong in this repo, that deserves a rule. If it hasn't failed at it, it probably doesn't need to be written down.
- **Place rules at the right level.** Module-specific conventions go in the nested module's `AGENTS.md`; cross-cutting standards go in `.agent-rules/`. Don't duplicate.

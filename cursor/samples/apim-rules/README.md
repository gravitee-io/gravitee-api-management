# APIM Rules for Cursor

This collection contains Gravitee APIM-specific **rules** in `.mdc` format for use with Cursor IDE.

> **Rules vs Skills**: Rules provide coding standards and patterns (HOW to write code). For executable tasks (building, starting APIM), see [`../apim-skills/`](../apim-skills/).

## What's Inside

### Java Rules

- **`architecture.mdc`** - Naming conventions, onion architecture, package structure
- **`vertx.mdc`** - Vert.x event loop patterns, non-blocking requirements
- **`code-style.mdc`** - Code style standards, main entry points, environment configuration
- **`rest-api-guidelines.mdc`** - REST API design standards, pagination, filtering
- **`error-handling.mdc`** - Diagnostic reporting, exception handling, logging best practices

## Installation

### Option 1: Copy to Your APIM Project

Copy the rules to your project's `.cursor/rules/` directory:

```bash
# From your APIM project root
mkdir -p .cursor/rules
cp -r /path/to/gravitee-antigravity-samples/cursor/samples/apim-rules/java/ .cursor/rules/
```

### Option 2: Symlink (Recommended for Development)

Create a symlink to always use the latest version:

```bash
# From your APIM project root
mkdir -p .cursor/rules
ln -s /path/to/gravitee-antigravity-samples/cursor/samples/apim-rules/java .cursor/rules/java
```

## Using Rules in Cursor

### Automatic Application

Rules with `alwaysApply: true` are automatically active when working with matching files.

For example, `architecture.mdc` automatically activates for all Java files in `gravitee-apim-*` modules.

### Manual Invocation

You can manually invoke any rule in your Cursor chat:

```
@architecture explain the onion architecture pattern
@vertx check if this code blocks the event loop
@rest-api-guidelines review this API endpoint
@error-handling improve this exception handling
```

## What Are Rules?

Rules are **persistent instructions** that guide AI behavior:
- Define coding standards and patterns
- Enforce architectural decisions
- Provide best practices and conventions
- **Do not execute** - they instruct HOW to write code

## File Format: .mdc

`.mdc` (Model Directed Code) is Cursor's native rule format with advanced features:

- **Glob patterns** - Auto-apply to specific files
- **Always apply flag** - Control persistent activation
- **Manual invocation** - Call rules via `@rule-name`
- **YAML frontmatter** - Structured metadata

Example frontmatter:

```yaml
---
globs: 
  - "**/gravitee-apim-*/**/*.java"
alwaysApply: true
description: "Rule description"
---
```

## Structure

The `java/` folder structure is designed to scale:

```
cursor/samples/apim-rules/
├── java/              # Java-specific rules
│   ├── architecture.mdc
│   ├── vertx.mdc
│   ├── code-style.mdc
│   ├── rest-api-guidelines.mdc
│   └── error-handling.mdc
└── (future: angular/, frontend/, etc.)
```

## Why APIM-Specific?

These rules are tailored for **Gravitee APIM projects** and reflect:

- APIM's onion architecture
- Gateway's Vert.x event loop requirements
- REST API team conventions
- Multi-module Maven structure

Other Gravitee projects may have different conventions.

## Related

**For executable tasks** (build, start, stop), see:
- [`../apim-skills/`](../apim-skills/) - Executable skills for development workflows

**For Antigravity format** (`.md` files), see:
- `antigravity/samples/sample-apim-rules/rules/java/`

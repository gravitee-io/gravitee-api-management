---
name: gh-setup-ci
description: Creates a GitHub Actions CI workflow with build, lint, and test phases. Use when adding continuous integration to a TypeScript project.
---

# Setup GitHub CI

Sets up a standard three-phase (Build, Lint, Test) GitHub Actions CI workflow.

## When to use this skill

- Adding CI/CD to a new repository
- Setting up GitHub Actions for a TypeScript project
- Creating automated build/test pipelines

## Instructions

### 1. Create the workflow directory

```bash
mkdir -p .github/workflows
```

### 2. Copy the CI template

| Source             | Destination                |
| ------------------ | -------------------------- |
| `resources/ci.yml` | `.github/workflows/ci.yml` |

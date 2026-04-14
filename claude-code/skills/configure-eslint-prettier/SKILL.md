---
name: configure-eslint-prettier
description: Configures ESLint (Flat Config) and Prettier to work together. Use when setting up code formatting and linting for a TypeScript project.
---

# Configure ESLint + Prettier

Adds Prettier and configures ESLint (Flat Config) to work harmoniously together.

## When to use this skill

- Setting up linting and formatting for a new project
- Migrating to ESLint Flat Config
- Integrating Prettier with ESLint

## Instructions

### 1. Install dependencies

```bash
pnpm add -D prettier eslint-config-prettier eslint-plugin-prettier
```

### 2. Copy configuration files

| Source                        | Destination         |
| ----------------------------- | ------------------- |
| `resources/.prettierrc`       | `.prettierrc`       |
| `resources/.prettierignore`   | `.prettierignore`   |
| `resources/eslint.config.mjs` | `eslint.config.mjs` |

---
name: configure-package-scripts
description: Adds standard development scripts to package.json including lint, format, build, and test commands. Use when standardizing npm scripts for a TypeScript project.
---

# Configure Package Scripts

Updates the `scripts` section of `package.json` with standard development commands.

## When to use this skill

- Standardizing npm scripts for a new project
- Adding missing development scripts
- Aligning scripts with team conventions

## Instructions

Add the following scripts to your `package.json`:

```json
{
    "scripts": {
        "dev": "next dev --turbo",
        "build": "next build",
        "start": "next start",
        "lint": "eslint .",
        "lint:fix": "eslint . --fix",
        "format": "prettier --check .",
        "format:fix": "prettier --write .",
        "test": "vitest run",
        "test:watch": "vitest",
        "test:coverage": "vitest run --coverage"
    }
}
```

Or use the CLI:

```bash
pnpm pkg set scripts.lint="eslint ."
pnpm pkg set scripts.lint:fix="eslint . --fix"
pnpm pkg set scripts.format="prettier --check ."
pnpm pkg set scripts.format:fix="prettier --write ."
pnpm pkg set scripts.test="vitest run"
pnpm pkg set scripts.test:watch="vitest"
pnpm pkg set scripts.test:coverage="vitest run --coverage"
```

---
name: add-vitest
description: Adds Vitest testing framework with React Testing Library and JSDOM. Use when setting up unit testing for a TypeScript or React project.
---

# Add Vitest

Adds the Vitest testing framework along with React Testing Library and JSDOM for component testing.

## When to use this skill

- Setting up unit testing for a new project
- Adding Vitest to an existing TypeScript/React project
- Replacing Jest with Vitest

## Instructions

### 1. Install dependencies

```bash
pnpm add -D vitest @vitest/coverage-v8 @vitejs/plugin-react @testing-library/react @testing-library/jest-dom @testing-library/dom jsdom
```

### 2. Copy configuration files

| Source                       | Destination        |
| ---------------------------- | ------------------ |
| `resources/vitest.config.ts` | `vitest.config.ts` |
| `resources/vitest.setup.ts`  | `vitest.setup.ts`  |

### 3. Create test directory

```bash
mkdir -p src/__tests__
```

### 4. Add placeholder test

| Source                    | Destination                   |
| ------------------------- | ----------------------------- |
| `resources/setup.test.ts` | `src/__tests__/setup.test.ts` |

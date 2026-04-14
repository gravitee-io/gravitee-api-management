---
name: create-nextjs-base
description: Creates a new Next.js project with TypeScript, ESLint, Tailwind, App Router, and Turbopack. Use when starting a new web application.
---

# Create Next.js Base

Creates a new Next.js project with standard opinionated flags.

## When to use this skill

- Starting a new web application
- Creating a Next.js project with standard tooling
- Initializing a TypeScript React project

## Instructions

Run the following commands to initialize the project:

```bash
npx create-next-app@latest <app-name> --ts --eslint --tailwind --app --src-dir --turbopack
cd <app-name>
rm package-lock.json
pnpm install
```

## Flags Explained

| Flag          | Purpose                  |
| ------------- | ------------------------ |
| `--ts`        | TypeScript support       |
| `--eslint`    | ESLint integration       |
| `--tailwind`  | Tailwind CSS integration |
| `--app`       | App Router support       |
| `--src-dir`   | Use `src/` directory     |
| `--turbopack` | Enable Turbopack         |

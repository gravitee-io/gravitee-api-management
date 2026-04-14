---
name: init-next-project
description: Initialize a new Next.js project with standard tooling
---

# /init-next-project

Use this skill to set up a new Next.js project with all standard tooling configured. This skill leverages atomic skills for consistent configuration.

## Prerequisites

- Node.js 20+ installed
- pnpm installed (`corepack enable`)
- GitHub CLI (`gh`) authenticated

## User Questions

1. **Project location?** (e.g., `/Users/you/workspace/org-name/` -- avoid hidden/config directories)
2. **Project name?** (e.g., `my-awesome-app`)
3. **Brief project description?** (for README)
4. **Open Source?** (yes/no -- determines if LICENSE, CONTRIBUTING, etc. are created)
5. **GitHub visibility?** (`--private` or `--public`)

---

## Steps

### 1. Create Base Project

Invoke the `/create-nextjs-base` skill to scaffold the project.

### 2. Configure ESLint + Prettier

Invoke the `/configure-eslint-prettier` skill to set up formatting and linting.

### 3. Add Vitest Testing Framework

Invoke the `/add-vitest` skill to configure the test runner and placeholder tests.

### 4. Update package.json Scripts

Invoke the `/configure-package-scripts` skill to add standard development scripts.

### 5. Create GitHub Actions CI

Invoke the `/gh-setup-ci` skill to add the build/lint/test pipeline.

### 6. (Optional) Create Open Source Files

If the project is Open Source, invoke the `/add-oss-boilerplate` skill.

### 7. Initialize GitHub Repository

Run the following command to create and push the repository:

```bash
gh repo create <owner>/<app-name> --private --source=. --remote=origin --push
```

### 8. Update README

Replace the default Next.js README with the project-specific description and standard sections for installation, scripts, and tech stack.

### 9. Verify Setup

Run all verification steps to confirm a clean build:

```bash
pnpm lint && pnpm format && pnpm build && pnpm test
```

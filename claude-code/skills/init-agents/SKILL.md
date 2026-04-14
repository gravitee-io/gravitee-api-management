---
name: init-agents
description: Add A2A agent infrastructure to a new or existing project
---

# /init-agents

Use this skill to add **A2A (Agent-to-Agent)** protocol-compliant agent infrastructure to projects. This skill leverages composable skills for consistent configuration.

## Modes

- **STANDALONE**: Creates a new TypeScript project with full tooling
- **ADDON**: Adds agent infrastructure to an existing project (TypeScript, Python, or Java)

## User Questions

1. **Project location?** (e.g., `/Users/you/workspace/org-name/` -- avoid hidden/config directories)
2. **Mode?** (`standalone` or `addon`)
3. **Agent team name?** (e.g., `spectral-agent`, `diagram-architect`)
4. **Primary skill domain?** (for agent card description)

---

# STANDALONE Mode

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

### 6. Add A2A Infrastructure (Next.js Adapter)

Invoke the `/add-a2a-infrastructure` skill to add the LLM provider and LangGraph nodes, **BUT** with the following adjustments for Next.js:

1.  **Dependencies**: Do NOT install `express`, `cors`, or `body-parser`. Ensure `dotenv` is handled by Next.js.
2.  **Server**: Do NOT create `src/app.ts` (Express server). Instead, expose the agent via a Next.js API Route (e.g., `src/app/api/agent/route.ts`) or use the A2A SDK's Next.js adapter if available.
3.  **Resources**: Copy the core logic files (`provider.ts`, `agent.ts`, `graph/`) but adapt the entry point.

### 7. Initialize GitHub Repository

Run the following command to create and push the repository:

```bash
gh repo create <owner>/<app-name> --private --source=. --remote=origin --push
```

### 8. Update README

Replace the default Next.js README with the project-specific description. Add agent-specific sections for endpoints and running instructions.

### 9. Verify Setup

Run all verification steps to confirm a clean build and agent health:

```bash
pnpm lint && pnpm format && pnpm build && pnpm test
# Start dev server and verify endpoint
pnpm dev
# In separate terminal
curl http://localhost:3000/.well-known/agent.json
```

---

# ADDON Mode

### 1. Auto-Detection

Initialize detection of the project type (TypeScript, Python, or Java) by checking for `package.json`, `pom.xml`, or `pyproject.toml`.

### 2. Add A2A Infrastructure

Invoke the `/add-a2a-infrastructure` skill (passing the detected language context).

### 3. Verify

Run existing build/test commands and verify the agent card endpoint.

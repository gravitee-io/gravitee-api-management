---
name: gravitee-init-project
description: Initialize a new Gravitee Java project with standard tooling
---

# /gravitee-init-project

Use this skill to set up a new Gravitee Java project aligned with official standards.

## Prerequisites

- Java 21+
- Maven 3.8+
- GitHub CLI (`gh`) authenticated

## User Questions

1.  **Project location?** (must be `~/workspace/Gravitee/`)
2.  **Project name?** (e.g., `gravitee-policy-my-new-policy` -- must start with `gravitee-`)
3.  **Project type?** (policy | endpoint | entrypoint | resource | multi-module | standalone)
4.  **Brief project description?**
5.  **GroupId pattern?** (io.gravitee | com.graviteesource)
6.  **GitHub organization?** (`gravitee-io` or `gravitee-io-labs`)
7.  **GitHub visibility?** (`--private` or `--public`)

---

## Steps

### 1. Initialize Project Directory

Create and enter the project directory in the standard Gravitee workspace:

```bash
mkdir -p ~/workspace/Gravitee/<project-name>
cd ~/workspace/Gravitee/<project-name>
```

Ensure the project name follows the `gravitee-*` convention and aligns with the selected type.

### 2. Create Base Maven Project

Invoke the `/gravitee-create-maven-base` skill.

### 3. Add Plugin Structure (If applicable)

If the project type is `policy`, `endpoint`, `entrypoint`, or `resource`, invoke the `/gravitee-add-plugin-structure` skill.

### 4. Add CI/CD and Templates

Invoke the following skills:

- **CircleCI**: Invoke the `/gravitee-add-circleci` skill
- **GitHub Files**: Invoke the `/gravitee-add-github-files` skill
- **Gitignore**: Invoke the `/gravitee-add-gitignore` skill
- **License**: Invoke the `/gravitee-add-license` skill
- **Prettier**: Invoke the `/gravitee-add-prettier` skill

### 5. Create Skeleton Java Class

Based on the project type, create a main class and its configuration.

- Path: `src/main/java/{group}/{type}/{name}/{MainClass}.java`

### 6. Initialize GitHub Repository

```bash
git init
git add .
git commit -m "chore: initial gravitee project structure"
gh repo create <org-name>/<project-name> --<visibility> --source=. --remote=origin --push
```

### 7. Verify

```bash
mvn clean verify
```

---
name: gravitee-add-github-files
description: Adds standard Gravitee GitHub templates (PR, Renovate, CODEOWNERS).
---

# Add Gravitee GitHub Files

This skill adds standard repository management files used by the Gravitee team.

## Instructions

1.  **Create Directory**:

    ```bash
    mkdir -p .github
    ```

2.  **Add pull_request_template.md**:
    Create `.github/pull_request_template.md` using the template at `resources/pull_request_template.md.template`.

3.  **Add renovate.json**:
    Create `.github/renovate.json` using the template at `resources/renovate.json.template`.

4.  **Add CODEOWNERS**:
    Create `.github/CODEOWNERS` using the template at `resources/CODEOWNERS.template`.

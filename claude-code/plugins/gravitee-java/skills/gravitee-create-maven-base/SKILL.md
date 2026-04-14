---
name: gravitee-create-maven-base
description: Scaffolds a base Maven project with Gravitee standards (parent POM, BOM, and structure).
---

# Create Gravitee Maven Base

This skill initializes a standard Maven project structure aligned with Gravitee's conventions.

## Instructions

1.  **Create Directory Structure**:

    ```bash
    mkdir -p src/main/java src/main/resources src/test/java src/test/resources
    ```

2.  **Generate pom.xml**:
    Create a `pom.xml` using the template at `resources/pom.xml.template`. Replace placeholders like `${groupId}`, `${artifactId}`, `${packaging}`, `${projectName}`, and `${description}`.

3.  **Lombok Config**:
    Create `lombok.config` in the root using the template at `resources/lombok.config.template`.

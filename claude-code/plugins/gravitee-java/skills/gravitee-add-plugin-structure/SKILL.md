---
name: gravitee-add-plugin-structure
description: Adds Gravitee plugin-specific files (plugin.properties, assembly XML, and schemas).
---

# Add Gravitee Plugin Structure

This skill adds the necessary configuration for a Maven project to be recognized as a Gravitee plugin.

## Instructions

1.  **Create resources**:

    ```bash
    mkdir -p src/main/resources/schemas
    mkdir -p src/main/resources/images
    mkdir -p src/main/assembly
    ```

2.  **Add plugin.properties**:
    Create `src/main/resources/plugin.properties` using the template at `resources/plugin.properties.template`.

3.  **Add Assembly XML**:
    Create `src/main/assembly/plugin-assembly.xml` using the template at `resources/plugin-assembly.xml.template`.

4.  **Update pom.xml**:
    Add the `maven-assembly-plugin` configuration to the `<build><plugins>` section (refer to `SKILL.md` for the XML snippet).

5.  **Add Schema Placeholder**:
    Create `src/main/resources/schemas/schema-form.json` using the template at `resources/schema-form.json.template`.

---
name: add-oss-boilerplate
description: Adds open-source documentation including Apache 2.0 License, Contributor Covenant Code of Conduct, and Contributing guide. Use when making a project open source.
---

# Add OSS Boilerplate

Adds standard open-source documentation including Apache 2.0 License, Code of Conduct, and Contributing guide.

## When to use this skill

- Making a private project open source
- Setting up a new open-source repository
- Adding missing OSS documentation

## Instructions

Copy the following files from `resources/` to the project root:

| Source                         | Destination          |
| ------------------------------ | -------------------- |
| `resources/LICENSE`            | `LICENSE`            |
| `resources/CODE_OF_CONDUCT.md` | `CODE_OF_CONDUCT.md` |
| `resources/CONTRIBUTING.md`    | `CONTRIBUTING.md`    |

Replace placeholders:

- `<year>` with current year
- `<fullname>` with copyright holder
- `<Project Name>` with actual project name

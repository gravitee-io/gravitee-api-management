# Cursor Resources

A curated collection of resources for the **[Cursor](https://cursor.com)** AI code editor.

---

## 📁 Resource Types

### Rules

Cursor uses rule files to guide AI behavior within a project.

| Type              | Location              | Description                                |
| ----------------- | --------------------- | ------------------------------------------ |
| **Project Rules** | `.cursor/rules/*.mdc` | Version-controlled, project-specific rules |
| **Legacy Rules**  | `.cursorrules`        | Single file in project root (deprecated)   |
| **User Rules**    | Cursor Settings       | Global rules for all projects              |
| **Team Rules**    | Dashboard             | Enterprise/Team plan only                  |

### AGENTS.md

A simpler alternative to the `.cursor/rules/` directory. Place an `AGENTS.md` file in your project root or subdirectories to provide agent instructions.

### Skills

Agent Skills are portable, version-controlled packages that teach agents how to perform domain-specific tasks.

| Scope               | Location            |
| ------------------- | ------------------- |
| **Project Skills**  | `.cursor/skills/`   |
| **Personal Skills** | `~/.cursor/skills/` |

> **Note:** Skills include instructions and optional executable scripts that extend agent capabilities.

---

## 📂 Local Storage Locations

```
~/.cursor/
├── skills/              # Personal skills

<project-root>/
├── .cursorrules         # Legacy rules file (deprecated)
├── AGENTS.md            # Agent instructions (alternative to rules)
└── .cursor/
    ├── rules/           # Project rules (*.mdc files)
    └── skills/          # Project skills
```

---

## 📦 Samples

Available rule samples you can use in your projects:

### Common Rules (`samples/rules/`)

- **[Angular 20+ Rules](samples/rules/angular.mdc)**: Comprehensive Angular 20+ coding standards covering Signals, Standalone Components, Typed Forms, and modern Angular patterns.

### APIM Rules (`samples/apim-rules/`)

Gravitee APIM-specific **coding standards and patterns** in `.mdc` format:

- **Java Rules** - Architecture, Vert.x patterns, REST API guidelines, error handling, code style
- Define HOW to write code (naming conventions, design patterns, best practices)
- Ready to copy to `.cursor/rules/` in your APIM project
- See [`samples/apim-rules/README.md`](samples/apim-rules/README.md) for installation instructions

### APIM Skills (`samples/apim-skills/`)

Gravitee APIM-specific **executable workflows** for development tasks:

- **build-apim** - Maven build commands
- **start-apim** - Start full APIM stack locally
- **stop-apim** - Stop all APIM services
- **format-code** - Format code and add license headers
- Ready to copy to `.cursor/skills/` in your APIM project
- See [`samples/apim-skills/README.md`](samples/apim-skills/README.md) for installation instructions

---

## 📚 Official Documentation

- [Cursor Rules Documentation](https://docs.cursor.com/context/rules)
- [Project Rules](https://docs.cursor.com/context/rules#project-rules)
- [Agent Skills](https://docs.cursor.com/context/agent-skills)

---

## 🚀 Usage

Place your `.mdc` rule files or skill directories in `samples/` to share with the community.

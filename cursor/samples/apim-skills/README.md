# APIM Skills for Cursor

This collection contains Gravitee APIM-specific **skills** for use with Cursor IDE.

> **Skills vs Rules**: Skills are executable capabilities that perform tasks. For coding standards and patterns, see [`../apim-rules/`](../apim-rules/).

## What Are Skills?

Skills are **executable capabilities** that agents can invoke:
- Perform specific development tasks
- Automate workflows
- Execute scripts and commands
- Can include executable code in `scripts/` directories

## Available Skills

| Skill | Description | Invoke With |
|-------|-------------|-------------|
| **build-apim** | Maven build commands for APIM | `/build-apim` |
| **start-apim** | Start full APIM stack (Gateway, REST API, Console, DB) | `/start-apim` |
| **stop-apim** | Stop all APIM services gracefully | `/stop-apim` |
| **format-code** | Format code with Prettier and add license headers | `/format-code` |

## Installation

### Option 1: Copy to Your APIM Project

Copy the skills to your project's `.cursor/skills/` directory:

```bash
# From your APIM project root
mkdir -p .cursor/skills
cp -r /path/to/gravitee-antigravity-samples/cursor/samples/apim-skills/* .cursor/skills/
```

### Option 2: Symlink (Recommended for Development)

Create a symlink to always use the latest version:

```bash
# From your APIM project root
mkdir -p .cursor/skills
ln -s /path/to/gravitee-antigravity-samples/cursor/samples/apim-skills/* .cursor/skills/
```

### Option 3: User-Level Skills (Global)

Install skills globally for all your projects:

```bash
mkdir -p ~/.cursor/skills
cp -r /path/to/gravitee-antigravity-samples/cursor/samples/apim-skills/* ~/.cursor/skills/
```

## Using Skills

### Manual Invocation

Invoke skills explicitly using `/skill-name` in Cursor chat:

```
/build-apim quick build without tests
/start-apim start the development environment  
/stop-apim shutdown all services
/format-code format all Java files
```

### Automatic Invocation

Skills are automatically suggested when Agent determines they are relevant based on context and conversation.

## Skill Structure

Each skill follows this structure:

```
skill-name/
├── SKILL.md           # Main skill definition with YAML frontmatter
└── scripts/           # Optional: executable scripts
    └── helper.sh
```

### SKILL.md Format

```yaml
---
name: skill-name
description: What the skill does and when to use it
---

# Skill Name

Detailed instructions for the agent...
```

## Examples

### Build Project

```
User: "Build the project without tests"
Agent: [Invokes /build-apim]
       Running: mvn clean install -DskipTests -Dskip.validation -T 2C
```

### Start Development Environment

```
User: "Start APIM for local development"
Agent: [Invokes /start-apim]
       Starting MongoDB, Elasticsearch, Gateway, REST API, Console UI...
```

### Format Code Before Commit

```
User: "Format code and add license headers"
Agent: [Invokes /format-code]
       Running: mvn prettier:write && mvn license:format
```

## Skill Details

### build-apim

Maven build commands with various profiles and options:
- Quick build (skip tests)
- Full build (with tests)
- Build with plugins
- Format and license commands

### start-apim

Complete development stack startup:
- Starts MongoDB and Elasticsearch containers
- Launches Gateway (port 8082)
- Launches REST API (port 8083)
- Launches Console UI (port 4000)
- Includes helper script: `scripts/start-stack.sh`

### stop-apim

Graceful shutdown of all components:
- Port-based process identification
- SIGTERM → SIGKILL fallback
- Stops Docker containers
- Verifies all services stopped

### format-code

Code formatting and licensing:
- Prettier formatting (140 width, 4 spaces)
- Apache 2.0 license headers
- Pre-commit hook compatible

## Why Skills?

Skills differ from Rules in purpose:

| Aspect | Rules | Skills |
|--------|-------|--------|
| Purpose | Define HOW to write code | Execute tasks |
| Content | Standards, patterns, guidelines | Commands, scripts, workflows |
| Execution | Never executed | Can be executed |
| Example | "Use kebab-case for URIs" | "mvn clean install" |
| Location | `.cursor/rules/` | `.cursor/skills/` |

## Troubleshooting

**Skill not appearing**: Ensure the skill folder name matches the `name` field in YAML frontmatter.

**Script permission denied**: Make scripts executable:
```bash
chmod +x .cursor/skills/*/scripts/*.sh
```

**Skill not auto-invoked**: Skills can be disabled from auto-invocation by setting `disable-model-invocation: true` in frontmatter.

## Related

**For coding standards** (architecture, patterns), see:
- [`../apim-rules/`](../apim-rules/) - APIM coding rules and guidelines

**For Antigravity workflows**, see:
- `antigravity/samples/sample-apim-rules/workflows/`

## Learn More

Agent Skills is an open standard: [agentskills.io](https://agentskills.io)

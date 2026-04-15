---
name: build-apim
description: Build Gravitee APIM project with Maven. Use when building, compiling, or packaging the project, or when the user mentions "build", "compile", or "mvn".
---

# Build APIM

Build the Gravitee APIM project using Maven.

## When to Use

Use this skill when:
- User asks to build the project
- Need to compile after code changes
- Creating distribution bundles
- Formatting code or adding license headers

## Build Commands

### Quick Build (Skip Tests)

Fastest build for development. Skips tests and validation.

```bash
mvn clean install -DskipTests -Dskip.validation -T 2C
```

Use this when:
- Making quick iterations
- Just need to compile without running tests
- Time is critical

### Full Build

Complete build with all tests and validations.

```bash
mvn clean install -T 2C
```

Use this when:
- Preparing for commit/PR
- Need to ensure all tests pass
- Running full validation

### Build with Plugins

Build with default and development plugin bundles.

```bash
mvn clean install -T 2C -P bundle-default,bundle-dev
```

Use this when:
- Need plugin bundles for development
- Testing with plugins enabled

## Code Formatting & Licensing

### Format Code with Prettier

```bash
mvn prettier:write
```

Formats all Java files according to project Prettier configuration (140 print width, 4-space tabs).

### Add License Headers

```bash
mvn license:format
```

Adds Apache 2.0 license headers to all Java files that are missing them.

### Format and License Together

```bash
mvn prettier:write && mvn license:format
```

Run both commands to ensure code is properly formatted and licensed.

## Build Flags Explained

- `-DskipTests` - Skip running tests
- `-Dskip.validation` - Skip validation checks
- `-T 2C` - Use 2 threads per CPU core (parallel build)
- `-P bundle-default,bundle-dev` - Activate Maven profiles for plugin bundles

## Typical Build Time

- Quick build (no tests): 2-5 minutes
- Full build: 10-20 minutes
- First-time build: May take longer due to dependency downloads

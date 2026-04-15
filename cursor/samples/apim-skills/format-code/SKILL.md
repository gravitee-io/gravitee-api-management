---
name: format-code
description: Format Java code with Prettier and add Apache 2.0 license headers. Use when formatting code, fixing style issues, or when user mentions "format", "prettier", or "license headers".
---

# Format Code

Format all Java code according to APIM standards and add required Apache 2.0 license headers.

## When to Use

Use this skill when:
- Need to format Java code before committing
- Code style checks are failing
- License headers are missing
- User asks to "format code" or "fix style"
- Preparing code for PR review

## Format with Prettier

Format all Java files according to project Prettier configuration.

```bash
mvn prettier:write
```

### Prettier Configuration

- **Print width**: 140 characters
- **Indentation**: 4 spaces
- **Line endings**: LF (Unix style)

## Add License Headers

Add Apache 2.0 license headers to all Java files missing them.

```bash
mvn license:format
```

### License Header Template

```java
/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Format and License Together

Run both formatting and license check in sequence:

```bash
mvn prettier:write && mvn license:format
```

This ensures:
1. Code is properly formatted
2. All files have required license headers
3. Ready for commit

## Check Before Formatting

Check formatting without making changes:

```bash
# Check Prettier formatting
mvn prettier:check

# Check license headers
mvn license:check
```

Use these commands in CI/CD pipelines or before commits to verify formatting compliance.

## Scope

These commands affect:
- All `.java` files in the project
- Respects `.prettierignore` if present
- Skips generated code and dependencies

## Pre-commit Hook

Consider adding to pre-commit hook:

```bash
# .git/hooks/pre-commit
#!/bin/bash
mvn prettier:write license:format
git add -u
```

## Troubleshooting

**Prettier not installed**: Ensure Maven dependencies are resolved:
```bash
mvn dependency:resolve
```

**License format fails**: Check that `LICENSE` file exists in project root.

**Files still not formatted**: Check `.prettierignore` - files may be intentionally excluded.

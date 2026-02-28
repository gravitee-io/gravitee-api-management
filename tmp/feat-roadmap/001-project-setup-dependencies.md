# 001: Project Setup & Maven Dependencies

> **Common docs**: [architecture-overview.md](./architecture-overview.md) · [json-schema-llm-integration.md](./json-schema-llm-integration.md)

## Objective

Set up the Maven dependency infrastructure so the Zee backend can import `json-schema-llm-java` and the `gravitee-api-definition-spec` Java SDK. Add the configuration properties class.

## Tasks

### 1. Create vendored `lib/` directory

Copy the required JARs from the local builds into a `lib/` directory at the APIM repo root using the standard Maven repository layout.

**Source JARs**:

- `json-schema-llm-java` from `~/workspace/dotslashderek/jsonschema-llm/` (after `mvn install -DskipTests`)
- `gravitee-api-definition-spec` V4 Java SDK from `~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/` (after `mvn compile`)

**Reference**: Check `~/workspace/Gravitee/gravitee-mcp-dynamic/lib/` and `~/workspace/Gravitee/gravitee-mcp-dynamic/.m2-local/` for the exact directory layout used previously.

```bash
# Example layout
lib/
└── com/jsonschema/llm/json-schema-llm-java/0.1.0-ALPHA/
    ├── json-schema-llm-java-0.1.0-ALPHA.jar
    └── json-schema-llm-java-0.1.0-ALPHA.pom
```

### 2. Add local file-based repository to `pom.xml`

In the `gravitee-apim-rest-api` module's POM (or the parent POM), add:

```xml
<repositories>
    <repository>
        <id>project-lib</id>
        <url>file://${maven.multiModuleProjectDirectory}/lib</url>
    </repository>
</repositories>
```

### 3. Add Maven dependencies

In `gravitee-apim-rest-api-service/pom.xml`:

```xml
<dependency>
    <groupId>com.jsonschema.llm</groupId>
    <artifactId>json-schema-llm-java</artifactId>
    <version>0.1.0-ALPHA</version>
</dependency>
```

### 4. Add configuration properties

Create `ZeeConfiguration.java` or add to existing APIM config to read:

```yaml
ai:
    zee:
        enabled: true
        azure:
            url: ${OPENAI_API_URL}
            apiKey: ${OPENAI_API_KEY}
        rateLimiting:
            maxRequestsPerMinute: 10
```

### 5. Verify build

```bash
cd gravitee-apim-rest-api
mvn compile -pl gravitee-apim-rest-api-service -DskipTests
```

## Acceptance Criteria

- [ ] `mvn compile` succeeds with the new dependencies
- [ ] `ZeeConfiguration` reads Azure URL + API key from env vars
- [ ] No other modules break

## Commit Message

```
feat(zee): add json-schema-llm and GAD spec SDK dependencies

Add vendored json-schema-llm-java and gravitee-api-definition-spec
Java SDK as local Maven dependencies. Add ZeeConfiguration for
Azure OpenAI connection properties.
```

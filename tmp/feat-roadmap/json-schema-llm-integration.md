# json-schema-llm Integration Guide

## What It Is

[json-schema-llm](https://github.com/dotslashderek/json-schema-llm) is a Rust-based 10-pass schema-to-schema compiler. It converts any JSON Schema into an LLM-compatible structured output schema, producing **(schema, codec) pairs** where the codec records every lossy transformation for full round-trip rehydration.

## How We Use It

We use TWO artifacts from this ecosystem:

### 1. `json-schema-llm-java` — The Engine

**Maven coordinates**: `com.jsonschema.llm:json-schema-llm-java:0.1.0-ALPHA`
**Source**: `~/workspace/dotslashderek/jsonschema-llm/` (locally as `jsonschema-llm`)

Provides:

- `LlmRoundtripEngine` — orchestrates convert → generate → rehydrate
- `OpenAIFormatter` — formats requests for OpenAI's structured output API
- `HttpTransport` — makes the actual HTTP call to the LLM provider
- `ProviderConfig` — URL + headers for the provider
- `RoundtripResult` — contains `.data()` (rehydrated JSON), `.isValid()`, `.warnings()`

### 2. `gravitee-api-definition-spec` Java SDK — Pre-Built Components

**Maven coordinates**: TBD (locally published, see gravitee-api-definition-spec repo)
**Source**: `~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/`

Provides 64 typed component classes (V4), each with:

- `.schema()` — pre-compiled LLM-compatible JSON Schema
- `.codec()` — sidecar codec for rehydration
- `.original()` — original Gravitee JSON Schema
- `.generate(prompt, engine)` — convenience method that wires all three

Components: `Flow`, `Plan`, `Api`, `Endpoint`, `Entrypoint`, `Step`, `Cors`, `Listener`, `HttpListener`, `SubscriptionListener`, `TcpListener`, `EndpointGroup`, etc.

## Maven Dependency Setup

Both dependencies are **vendored locally** (not on Maven Central). The pattern from `gravitee-mcp-dynamic`:

```xml
<!-- In pom.xml -->
<dependency>
    <groupId>com.jsonschema.llm</groupId>
    <artifactId>json-schema-llm-java</artifactId>
    <version>0.1.0-ALPHA</version>
</dependency>

<!-- Local file-based repo for vendored JARs -->
<repositories>
    <repository>
        <id>project-lib</id>
        <url>file://${maven.multiModuleProjectDirectory}/lib</url>
    </repository>
</repositories>
```

The JARs need to be copied into a `lib/` directory in the APIM repo with the standard Maven repo layout:

```
lib/
└── com/jsonschema/llm/json-schema-llm-java/0.1.0-ALPHA/
    ├── json-schema-llm-java-0.1.0-ALPHA.jar
    └── json-schema-llm-java-0.1.0-ALPHA.pom
```

**Reference**: The working pattern is in `~/workspace/Gravitee/gravitee-mcp-dynamic/lib/` and `~/workspace/Gravitee/gravitee-mcp-dynamic/.m2-local/`.

## Usage Pattern

```java
import com.jsonschema.llm.engine.*;
import io.gravitee.apim.definition.llm.v4.Flow;

// 1. Configure provider
var config = ProviderConfig.builder()
    .url(azureOpenAiUrl)  // Full URL including deployment + api-version
    .header("api-key", azureApiKey)
    .build();

// 2. Create engine
var engine = LlmRoundtripEngine.create(
    new OpenAIFormatter(),
    config,
    new HttpTransport()
);

// 3. Generate (one-liner per component!)
RoundtripResult result = Flow.generate(enrichedPrompt, engine);

// 4. Use the result
JsonNode rehydratedFlow = result.data();  // Original Gravitee shape
boolean valid = result.isValid();
List<String> warnings = result.warnings();
```

## Schema Details (Flow Example)

The Flow schema is 21KB / 669 lines and includes:

- `anyOf` nullable unions (every optional field)
- Discriminated `anyOf` for selectors (http, channel, condition, mcp)
- Nested Step objects with stringified JSON policy configurations
- Enums for HTTP methods, path operators, etc.
- `additionalProperties: false` and all fields `required` (OpenAI strict mode requirement)

## Phase 0 Validation Results

✅ **PASSED** — Azure OpenAI `gpt-5-nano` accepted the 21KB Flow schema in strict structured output mode and generated a valid rate-limit + transform-headers flow in 6,126 tokens. See `/tmp/zee-brainstorming/phase0-validation-results.md` for full details.

## Key Files

| File                          | Location                                                                                              |
| ----------------------------- | ----------------------------------------------------------------------------------------------------- |
| json-schema-llm source        | `~/workspace/dotslashderek/jsonschema-llm/`                                                           |
| GAD spec repo                 | `~/workspace/Gravitee/gravitee-api-definition-spec/`                                                  |
| Java SDK (V4)                 | `~/workspace/Gravitee/gravitee-api-definition-spec/sdks/java/v4/`                                     |
| Flow schema                   | `~/workspace/Gravitee/gravitee-api-definition-spec/generated/v4/openai-strict/$defs/Flow/schema.json` |
| Flow codec                    | `~/workspace/Gravitee/gravitee-api-definition-spec/generated/v4/openai-strict/$defs/Flow/codec.json`  |
| MCP-D integration (reference) | `~/workspace/Gravitee/gravitee-mcp-dynamic/`                                                          |
| MCP-D vendored JARs           | `~/workspace/Gravitee/gravitee-mcp-dynamic/lib/`                                                      |

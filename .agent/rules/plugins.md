---
description: Rules for developing Gravitee plugins (policies, resources)
trigger: always_on
---

# Plugin Development Rules

Gravitee policies and resources are developed as separate Maven plugin projects.

## Plugin Types

| Type       | Purpose                           | Example                                             |
| ---------- | --------------------------------- | --------------------------------------------------- |
| Policy     | Transform requests/responses      | `gravitee-policy-ai-retrieval-augmented-generation` |
| Resource   | Shared resources (caches, models) | `gravitee-resource-ai-model-text-embedding`         |
| Endpoint   | Backend connectors                | `gravitee-endpoint-agent-to-agent`                  |
| Entrypoint | Frontend connectors               | `gravitee-entrypoint-agent-to-agent`                |

## Project Structure

```
gravitee-policy-{name}/
├── pom.xml
├── README.md
├── src/
│   ├── main/java/io/gravitee/policy/{name}/
│   │   ├── {Name}Policy.java
│   │   └── configuration/
│   └── test/java/
└── target/
    └── gravitee-policy-{name}-{version}.zip
```

## Build & Deploy

```bash
# Build plugin
cd gravitee-policy-{name}
mvn clean install -DskipTests -Dskip.validation -T 2C

# Copy to Gateway distribution
cp -v ./target/*.zip $PROJECTS_DIR/gravitee-api-management/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution/plugins

# Copy to REST API distribution
cp -v ./target/*.zip $PROJECTS_DIR/gravitee-api-management/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/plugins
```

### Aliases (from onboarding-resources)

```bash
# Build and copy to both
alias plugin="apimvn && copy_plugin_gw && copy_plugin_rest"

# Gateway only
alias gw_plugin="apimvn && copy_plugin_gw"

# REST API only
alias rest_plugin="apimvn && copy_plugin_rest"
```

## Policy Phases

Policies can execute at different phases:

| Phase               | Description            |
| ------------------- | ---------------------- |
| `onRequest`         | Before backend call    |
| `onResponse`        | After backend response |
| `onMessageRequest`  | Message/event request  |
| `onMessageResponse` | Message/event response |

## Example Plugins

Located in `~/workspace/Gravitee/`:

- `gravitee-policy-ai-retrieval-augmented-generation` - RAG policy
- `gravitee-policy-ai-context-caching` - Semantic caching
- `gravitee-resource-ai-model-text-embedding` - Embedding models
- `gravitee-resource-ai-vector-store-aws-s3` - Vector store

## License

All plugins must have Apache 2.0 license headers:

```bash
mvn license:format
```

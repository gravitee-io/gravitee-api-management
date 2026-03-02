---
description: Start Gravitee APIM locally (Gateway + REST API + Infrastructure)
---

> **Read-Only**: This workflow does NOT modify any project files. All commands use existing scripts and configurations.

# Prerequisites

- Java 17+ (`sdk use java 21-tem`)
- Maven, Docker Desktop running
- Node 20.19.0 (`nvm use`)
- Backend built (distribution folders exist)

# Step 0: Install Plugins (Optional)

> **Skip this step** if you don't need any external plugins.

If the user requests plugins to be included (e.g., vector stores, AI policies), build and install them first.

See [Plugin Development Rules](../rules/plugins.md) for full details.

For each plugin project (located in `~/workspace/Gravitee/`):

```bash
# Build the plugin
cd ~/workspace/Gravitee/gravitee-{plugin-name}
mvn clean install -DskipTests -Dskip.validation -T 2C

# Copy to Gateway plugins
cp -v ./target/*.zip ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution/plugins/

# Copy to REST API plugins
cp -v ./target/*.zip ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/plugins/
```

Common AI plugins:

- `gravitee-resource-ai-vector-store-aws-s3` - S3 vector store
- `gravitee-resource-ai-model-text-embedding` - Text embedding models
- `gravitee-policy-ai-retrieval-augmented-generation` - RAG policy
- `gravitee-policy-ai-context-caching` - Semantic caching

# Step 1: Start Infrastructure (MongoDB + Elasticsearch)

If containers exist but are stopped:

```bash
docker start gio_apim_mongodb gio_apim_elasticsearch
```

If starting fresh:
// turbo

```bash
cd ~/workspace/Gravitee/gravitee-api-management
docker compose -f docker/quick-setup/mongodb/docker-compose.yml up mongodb elasticsearch -d
```

Verify healthy:

```bash
docker ps --filter "name=gio_apim" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected: MongoDB (27017), Elasticsearch (9200)

# Step 2: Build Backend (First Time Only)

Skip this step if `target/distribution` folders already exist.

// turbo

```bash
cd ~/workspace/Gravitee/gravitee-api-management
mvn clean install -DskipTests -Dskip.validation -T 2C
```

This creates distribution folders (5-10 min first time).

# Step 3: Start Gateway (Port 8082)

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution/bin
./gravitee
```

Keep this terminal running. Gateway: http://localhost:8082

# Step 4: Start REST API (Port 8083)

Open **new terminal**:

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/bin
./gravitee
```

Keep this terminal running. REST API: http://localhost:8083

# Step 5: Console UI

Open **new terminal**:

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-console-webui
nvm use && yarn && yarn serve
```

Console: http://localhost:4000

# Verification

```bash
# Gateway responding
curl http://localhost:8082/

# REST API health
curl http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT/apis

# Elasticsearch
curl http://localhost:9200/_cluster/health
```

**Browser Verification:**

1. Open http://localhost:4000 in your browser.
2. Verify the Console UI loads successfully.

# Shutdown

```bash
docker stop gio_apim_mongodb gio_apim_elasticsearch
# Ctrl+C in Gateway and REST API terminals
```

# Troubleshooting

**Container conflict**: Use `docker start` for existing containers, not `docker compose up`.

**Kubernetes errors in logs**: Normal - just means not running in K8s.

**No distribution folder**: Run Maven build step first.

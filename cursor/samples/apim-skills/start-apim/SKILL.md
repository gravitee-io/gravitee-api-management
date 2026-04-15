---
name: start-apim
description: Start the full Gravitee APIM stack locally (Gateway, REST API, Console UI, MongoDB, Elasticsearch). Use when starting development environment or when user mentions "start apim", "run apim", "launch apim".
---

# Start APIM Stack

Start the complete Gravitee APIM development environment locally.

## When to Use

Use this skill when:
- User wants to start the APIM development environment
- Need to test APIs locally
- Developing or debugging APIM features
- User mentions "start", "run", or "launch" APIM

## Prerequisites

Before starting APIM, ensure:
- **Java 17+** installed (`sdk use java 21-tem`)
- **Maven** installed
- **Docker Desktop** running
- **Node.js 20.19.0** installed (`nvm use`)
- **Backend built** (distribution folders exist - if not, use `/build-apim` first)

## Starting the Stack

### Option 1: Manual Step-by-Step

#### Step 1: Start Infrastructure

Start MongoDB and Elasticsearch containers.

**If containers exist but are stopped:**

```bash
docker start gio_apim_mongodb gio_apim_elasticsearch
```

**If starting fresh:**

```bash
cd ~/workspace/Gravitee/gravitee-api-management
docker compose -f docker/quick-setup/mongodb/docker-compose.yml up mongodb elasticsearch -d
```

**Verify containers are healthy:**

```bash
docker ps --filter "name=gio_apim" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected: MongoDB (27017), Elasticsearch (9200)

#### Step 2: Start Gateway (Port 8082)

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution/bin
./gravitee
```

Keep this terminal running. Gateway will be available at: http://localhost:8082

#### Step 3: Start REST API (Port 8083)

Open a **new terminal**:

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/bin
./gravitee
```

Keep this terminal running. REST API will be available at: http://localhost:8083

#### Step 4: Start Console UI (Port 4000)

Open a **new terminal**:

```bash
cd ~/workspace/Gravitee/gravitee-api-management/gravitee-apim-console-webui
nvm use && yarn && yarn serve
```

Console UI will be available at: http://localhost:4000

Default credentials: `admin` / `admin`

### Option 2: Use Helper Script

```bash
scripts/start-stack.sh
```

This script automates the startup process.

## Verification

### Command Line Verification

```bash
# Gateway responding
curl http://localhost:8082/

# REST API health
curl http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT/apis

# Elasticsearch cluster health
curl http://localhost:9200/_cluster/health
```

### Browser Verification

1. Open http://localhost:4000 in your browser
2. Login with `admin` / `admin`
3. Verify Console UI loads successfully

## Optional: Installing Plugins

If plugins are needed (AI policies, vector stores, etc.), build and install them before starting:

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

## Troubleshooting

**Container conflict**: Use `docker start` for existing containers, not `docker compose up`.

**Kubernetes errors in logs**: Normal - just means not running in Kubernetes environment.

**No distribution folder**: Run `/build-apim` first to create distribution folders.

**Port already in use**: Check if services are already running:
```bash
lsof -i :8082  # Gateway
lsof -i :8083  # REST API
lsof -i :4000  # Console UI
```

## Stopping the Stack

Use `/stop-apim` skill or see shutdown instructions there.

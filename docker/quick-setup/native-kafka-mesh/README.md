# APIM with Native Kafka — MESH virtual cluster

This is the MESH variant of the [`native-kafka/`](../native-kafka/) quick-setup. The gateway fronts **two independent backend Kafka clusters** as one **virtual cluster** that downstream clients see as a single bootstrap host (`global.kafka.local:9092`).

## What you get

- `gio_apim_mesh_kafka_eu` — single-node KRaft cluster, advertised as `kafka-eu:9091`
- `gio_apim_mesh_kafka_us` — single-node KRaft cluster, advertised as `kafka-us:9091`
- `gio_apim_mesh_gateway` — the Gravitee gateway with the Kafka native protocol enabled (port `9092` host-side, SNI-routed on `*.kafka.local`)
- The full APIM stack (`mongodb`, `elasticsearch`, `management-api`, `management-ui`, `portal-ui`, `mailhog`)
- `gio_apim_mesh_kafka_client` — Kafka CLI container, points exclusively at the gateway

## Prerequisites

- **EE license** — Kafka native protocol is an EE feature. Create a `./.license/` **directory** at the root of this quick-setup and drop your `license.key` (or whatever filename your license is named) inside it. The directory is mounted into the gateway and management-api containers.
- **Docker** with compose v2 plugin. Tested with Docker 24+.
- **Free host ports**: `8082`, `8083`, `8084`, `8085`, `8025`, `1025`, `9092`, `9200`.

## Run docker-compose

```bash
APIM_VERSION={APIM_VERSION} docker compose up -d
```

For latest internal images:

```bash
export APIM_REGISTRY=graviteeio.azurecr.io
export APIM_VERSION=master-latest
docker compose up -d
```

## 1. General configuration

### 1.1 Default Kafka domain — pre-seeded

The gateway's HOST routing mode resolves `*.kafka.local`, and the management plane must publish the same domain pattern so each API's host prefix lands on the right alias. **This is already pre-set via env var** on the `management_api` service in `docker-compose.yml`:

```yaml
gravitee_portal_kafkaDomain={apiHost}.kafka.local
```

You can verify (or change) it from the console: Organization → Entrypoints & Sharding Tags → Entrypoint Configuration → **Default Kafka Domain**.

> The value above maps to `Key.PORTAL_KAFKA_DOMAIN` in the management API (overridable system parameter). The `{apiHost}` placeholder is replaced at runtime with each API's entrypoint host prefix — so the `global` API resolves to `global.kafka.local:9092`.

### 1.2 Cluster permission (only needed for non-admin users)

The default `admin` user has full permissions. If you want non-admin users to manage Kafka Cluster / Virtual Cluster entities, grant the `CLUSTER` env-scoped permission on their role:

- Organization → Roles → USER → check `CLUSTER` (READ + UPDATE).

## 2. Register the two backend clusters

Console → **Kafka Clusters** → **Add Cluster**.

| # | Name | crossId | Connection bootstrap | Security |
|---|---|---|---|---|
| 1 | `kafka-eu` | `kafka-eu` | `kafka-eu:9091` | PLAINTEXT |
| 2 | `kafka-us` | `kafka-us` | `kafka-us:9091` | PLAINTEXT |

For each cluster:
1. Create the cluster entity (the `crossId` field is optional but useful to reference the cluster from config-as-code; the table uses the same value as the name for simplicity).
2. Open it → **Configuration** tab → add a connection. The connection **name is a free-text field** — use anything (e.g., `default`); just remember it because the virtual cluster will reference it by name in §3.
3. Set the bootstrap servers (above) and security protocol PLAINTEXT.
4. Save.

> The gateway is on the `kafka-mesh` network and reaches each broker via its docker hostname. PLAINTEXT keeps the demo simple — the gateway↔broker leg is inside the docker network; the client↔gateway leg is SSL via SNI.

## 3. Create the virtual cluster

Console → **Kafka Virtual Clusters** → **Add**.

- Name: `global`
- crossId: `global`
- Backends (Configuration tab):
  - `kafka-eu` / `default`
  - `kafka-us` / `default`

Save.

## 4. Create the Kafka API

Console → APIs → **Add API** → **Create API**.

1. Protocol: **Native Kafka**.
2. Name: `kafka-global`, version `1.0.0`.
3. **Entrypoints** → Host prefix: `global` (matches the gateway alias `global.kafka.local`).
4. **Endpoints** → choose endpoint type **Virtual Cluster** → pick `global` from the dropdown.
5. **Security** → keep the default **Keyless** plan (the demo focuses on MESH; auth plans work the same way as on the [`native-kafka/`](../native-kafka/) demo).
6. Review → **Save & Deploy API**.

## 5. Produce one topic per backend

To exercise the cross-cluster fan-out, create one topic on each backend (NOT via the gateway — directly on each broker so each cluster owns exactly one).

```bash
# On the EU backend
docker exec -it gio_apim_mesh_kafka_eu \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-eu:9091 \
  --create --topic orders-eu --partitions 1 --replication-factor 1

# On the US backend
docker exec -it gio_apim_mesh_kafka_us \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-us:9091 \
  --create --topic orders-us --partitions 1 --replication-factor 1
```

> ⚠️ **MESH topic creation routing**: if you create topics **through the gateway** instead of directly on each broker, they all land on the **first configured backend** (EU in this setup). The gateway has no affinity-rule layer today; "first reachable backend wins" is the contract. To pin a topic to a specific backend, create it directly on that broker as above.

Now produce a few messages via the gateway — note we're talking to `global.kafka.local:9092`, not to either backend directly. The `-i` flag (no `-t`) keeps the pipe stdin clean:

```bash
docker exec -i gio_apim_mesh_kafka_client bash -c \
  "printf 'order-eu-1\norder-eu-2\n' | /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server global.kafka.local:9092 \
    --producer.config config/kafka-keyless-plan-ssl.properties \
    --topic orders-eu"

docker exec -i gio_apim_mesh_kafka_client bash -c \
  "printf 'order-us-1\norder-us-2\n' | /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server global.kafka.local:9092 \
    --producer.config config/kafka-keyless-plan-ssl.properties \
    --topic orders-us"
```

## 6. Consume the merged stream

A single consumer subscribed to both topics, against the gateway:

```bash
docker exec -it gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server global.kafka.local:9092 \
    --consumer.config config/kafka-keyless-plan-ssl.properties \
    --include 'orders-(eu|us)' \
    --group analytics --from-beginning
```

You should see all four messages — two from EU, two from US — through a single client session.

## 7. Inspect what the gateway built behind the scenes

After the consumer in §6 has joined and committed at least once, the gateway has created one **shadow group** per backend for the logical `analytics` group, named `analytics__shadow-c<clusterIndex>`. Verify directly on each backend:

```bash
docker exec -it gio_apim_mesh_kafka_eu \
  /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka-eu:9091 --list
# expect: analytics__shadow-c0

docker exec -it gio_apim_mesh_kafka_us \
  /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka-us:9091 --list
# expect: analytics__shadow-c1
```

But via the gateway, the shadow suffix is stripped — clients only ever see the logical name:

```bash
docker exec -it gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --list
# expect: analytics
```

## Useful commands

All commands target `global.kafka.local:9092` — i.e., they go through the gateway as a regular Kafka client would. The notes call out what the gateway does behind the scenes when it differs from a single-cluster setup.

### Topics

```bash
# List topics (merged view across both backends, no shadow suffix on client side)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --list

# Describe a topic (partitions, leader, ISR — resolved on the owning backend)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --describe --topic orders-eu

# Increase partitions (CreatePartitionsRouter routes to the topic's owning backend)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --alter --topic orders-eu --partitions 3

# Delete topics — batch is split per-backend and re-merged
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --delete --topic orders-eu --topic orders-us
```

### Topic configuration

```bash
# Read a topic's current config (cleanup.policy, retention.ms, etc.)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-configs.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --entity-type topics --entity-name orders-eu --describe

# Set per-topic retention (AlterConfigs is routed to the topic's owning backend)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-configs.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --entity-type topics --entity-name orders-eu \
    --alter --add-config retention.ms=600000
```

### Consumer groups

```bash
# List groups — gateway strips the __shadow-c<N> suffix; you only see logical names
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --list

# Describe a group (members, current offsets, lag — merged across shadow groups)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --describe --group analytics

# Reset offsets to the beginning (per-partition, routed to each topic's owning backend)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --reset-offsets --to-earliest --execute \
    --group analytics --topic orders-eu --topic orders-us

# Delete a group — fan-out to every backend that hosts a shadow, then merge
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --delete --group analytics
```

### Records & offsets

```bash
# Read the last N records from a topic (one-shot, no group commit)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server global.kafka.local:9092 \
    --consumer.config config/kafka-keyless-plan-ssl.properties \
    --topic orders-eu --from-beginning --max-messages 5

# End offsets per partition — useful for lag math without a consumer group
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-get-offsets.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --topic orders-eu --time -1
```

### Cluster metadata

```bash
# Describe the virtual cluster — merged broker list with virtual ids (10000, 20000, ...)
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-broker-api-versions.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
  | head -20

# Cluster id (virtual one — set by the gateway, not the backend's real Kraft id).
# Note: kafka-cluster.sh uses --config (not --command-config like the other tools).
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-cluster.sh cluster-id \
    --bootstrap-server global.kafka.local:9092 \
    --config config/kafka-keyless-plan-ssl.properties
```

### Reset between runs (without rebuilding the stack)

If you want to redo the walkthrough without `docker compose down -v` (which wipes everything):

```bash
# Delete the topics
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --delete --topic orders-eu --topic orders-us

# Delete the consumer group
docker exec -i gio_apim_mesh_kafka_client \
  /opt/kafka/bin/kafka-consumer-groups.sh \
    --bootstrap-server global.kafka.local:9092 \
    --command-config config/kafka-keyless-plan-ssl.properties \
    --delete --group analytics
```

### Not supported on MESH (will fail)

```bash
# Transactions — no multiplex handler; a transactional producer won't get a usable real PID
producer.config: transactional.id=anything   # → will misbehave; do not use on Virtual Cluster

# ACL CRUD — refused with SECURITY_DISABLED
kafka-acls.sh --bootstrap-server global.kafka.local:9092 --list
# → SecurityDisabledException
```

## Notes & limitations

- Topic names must be **distinct across backends**. Two backends with a same-named topic collapse into one from the client's view; the gateway picks one owner.
- **Idempotent producer** (`enable.idempotence=true`) works on MESH — the gateway allocates a virtual PID and rewrites each batch in place.
- **Transactions** are not supported on MESH (no multiplex handler for the transactional APIs).
- **ACL CRUD** on the virtual cluster is refused with `SECURITY_DISABLED` — manage ACLs directly on each backend.

## Cleanup

```bash
docker compose down -v
```

This stops every container and wipes the named volumes (mongo data, ES data, both kafka data dirs). Re-running `docker compose up -d` after a `down -v` gives you a fresh stack.

## See also

- [`../native-kafka/`](../native-kafka/) — Single-backend Kafka API pass-through. Start here if you want the simpler scenario before adding the MESH layer.

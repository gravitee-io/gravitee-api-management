# APIM with Native Kafka

A single-backend Kafka API behind the Gravitee gateway. The gateway exposes port `9092` to clients and pass-through forwards to one Apache Kafka broker. Use this as the **simplest** starting point — for a multi-backend MESH setup, see [`../native-kafka-mesh/`](../native-kafka-mesh/).

## What you get

- `gio_apim_kafka` — single-node KRaft Kafka broker (Apache Kafka 3.9.1), four security listeners (PLAINTEXT / SSL / SASL_PLAINTEXT / SASL_SSL)
- `gio_apim_gateway` — Gravitee gateway with the Kafka native protocol enabled; SNI-routes `*.kafka.local` to the deployed API
- Full APIM stack (`mongodb`, `elasticsearch`, `kibana`, `management-api`, `management-ui`, `portal-ui`, `mailhog`)
- `gio_apim_kafka-ui` — Provectus Kafka UI on `http://localhost:9003`, attached directly to the broker (useful to inspect topics / messages)
- `gio_apim_kafka-client` — Kafka CLI container scoped to the gateway side (`*.kafka.local`)

⚠️ Enterprise license required (Kafka native protocol is an EE feature). Drop your license file in `./.license`.

The gateway uses a self-signed wildcard certificate for `*.kafka.local` (in `./.ssl/`); clients trust it via `./.ssl/server.truststore.jks`.

## Run docker-compose

```bash
APIM_VERSION={APIM_VERSION} docker-compose up -d
```

For a clean reinstall:

```bash
export APIM_VERSION={APIM_VERSION}
docker-compose down -v
docker-compose pull
docker-compose up
```

For latest internal images:

```bash
export APIM_REGISTRY=graviteeio.azurecr.io
export APIM_VERSION=master-latest
docker-compose up -d
```

## 1. General configuration

The gateway runs in HOST routing mode (the default) and resolves API hostnames via SNI. The default Kafka domain must be set:

1. Console: `http://localhost:8084/` (`admin` / `admin`).
2. Organization → Entrypoints & Sharding Tags → Entrypoint Configuration → **Default Kafka Domain** = `kafka.local`.

The gateway is already configured with `gravitee_kafka_routingHostMode_defaultDomain=kafka.local`; the console value above mirrors that to the management plane.

## 2. Create a Native Kafka API

Console → APIs → **Add API** → **Create API**.

1. **Protocol**: Native Kafka.
2. **General**: name = `foo`, version = `1.0.0`.
3. **Entrypoints** → Host prefix: `foo` (or `bar` — see the note below).
4. **Endpoints** — pick the security profile you want to demonstrate:

   | Profile | Bootstrap | Security | Extra config |
   |---|---|---|---|
   | PLAINTEXT | `kafka:9091` | PLAINTEXT | none |
   | SSL | `kafka:9094` | SSL | Truststore "JKS With Path" = `./ssl/kafka-client.truststore.jks`, password `password` |
   | SASL_PLAINTEXT | `kafka:9092` (SASL listener) | SASL_PLAINTEXT | mechanism + credentials (see broker config in `.config/server.properties`) |
   | SASL_SSL | `kafka:9095` | SASL_SSL | both SSL + SASL config |

5. **Security** → keep the default **Keyless** plan (auth can be added later — see §4).
6. Review → **Save & Deploy API**.

> Only the host prefixes **`foo`** and **`bar`** work out of the box. The gateway service in `docker-compose.yml` declares both as DNS aliases. To add `baz`, append both `baz.kafka.local` and `broker-0-baz.kafka.local` to `services.gateway.networks.gateway.aliases`. (In a real deployment with a real wildcard cert this DNS plumbing is invisible.)

## 3. Produce & consume

The Kafka client container is pre-configured with the SSL truststore. The shipped property file `config/kafka-keyless-plan-ssl.properties` is keyless + SSL — usable as is for step 2.

```bash
docker exec -it gio_apim_kafka-client \
  /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server foo.kafka.local:9092 \
    --producer.config config/kafka-keyless-plan-ssl.properties \
    --topic client-topic-1
```

```bash
docker exec -it gio_apim_kafka-client \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server foo.kafka.local:9092 \
    --consumer.config config/kafka-keyless-plan-ssl.properties \
    --topic client-topic-1 --from-beginning
```

You can cross-check topic state in the Kafka UI at `http://localhost:9003`.

### Via the developer portal (optional)

To see the API in the new developer portal:
1. Settings → Settings → Enable the New Developer Portal.
2. On the API page: **Publish the API**.
3. Open `http://localhost:8085/next/`.
4. Open the API → Learn more → Subscribe → Keyless → Next.

The portal shows the same client config you've already been using.

## 4. Secure with an API Key plan (optional)

API Key (plus JWT and OAuth2, which require external IdP setup outside the scope of this demo) replaces the Keyless plan.

1. API page → **Consumer** → **Plan** → **Add new Plan** → **API Key**.
2. Set a name (nothing Kafka-specific here) → create the plan.
3. **Publish the plan**. A dialog confirms closing the unsecured Keyless plan — you cannot mix Keyless with a secure plan.
4. Redeploy the API (it will show as "out of sync").
5. Create a subscription: API Consumer → Subscriptions → **Create Subscription** → search "Default Application" → select the API Key plan → **Create**. Copy the API key.

The Kafka SASL/PLAIN mapping the gateway expects is:
- **username** = MD5 hex digest of the API key
- **password** = the API key itself

```bash
# macOS
echo -n "<API_KEY>" | md5
# Linux
echo -n "<API_KEY>" | md5sum
# Portable (requires openssl)
echo -n "<API_KEY>" | openssl md5
```

Open `./.kafka-client-config/kafka-api-key-plan-ssl.properties` and put the API key (password) and its MD5 (username) into the `sasl.jaas.config` line, then:

```bash
docker exec -it gio_apim_kafka-client \
  /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server foo.kafka.local:9092 \
    --producer.config config/kafka-api-key-plan-ssl.properties \
    --topic client-topic-1
```

```bash
docker exec -it gio_apim_kafka-client \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server foo.kafka.local:9092 \
    --consumer.config config/kafka-api-key-plan-ssl.properties \
    --topic client-topic-1 --from-beginning
```

> The portal's "My Subscription" page also displays the ready-to-paste client config — useful if you want to share the API with a real consumer team.

## Notes

- **Idempotent producer** (`enable.idempotence=true`) — fully supported on single-backend; the broker handles it natively.
- **Transactions** — supported on single-backend (pure pass-through); not supported on MESH ([see `native-kafka-mesh/`](../native-kafka-mesh/)).
- **Routing mode** — this setup uses HOST mode (SNI). If you need PORT mode (per-plan bootstrap port, no SNI), set `kafka.routingMode=port` on the gateway and use per-plan bootstrap ports.

## Cleanup

```bash
docker-compose down -v
```

## See also

- [`../native-kafka-mesh/`](../native-kafka-mesh/) — Same gateway, two backend clusters fronted as one MESH virtual cluster.
- [`../kafka-console/`](../kafka-console/) — Cluster-entity management + Kafka Console UI.

# plugin.properties (Plugin Manifest)

The plugin manifest is located at `src/main/resources/plugin.properties` and declares the policy to the Gravitee gateway plugin system.

## Template

```properties
id=kafka-<POLICY_ID>
name=Kafka <POLICY_DISPLAY_NAME>
version=${project.version}
description=${project.description}
class=<FULLY_QUALIFIED_POLICY_CLASS>
type=policy
category=others

documentation=DOCUMENTATION.adoc
schema=schema-form.json

native_kafka=<PHASES>
native_kafka.documentation=DOCUMENTATION.adoc
native_kafka.schema=schema-form.json

feature=apim-native-kafka-policy-<POLICY_ID>
```

## Fields

| Field | Description | Required |
|-------|-------------|----------|
| `id` | Unique policy identifier, used in API definitions. Convention: `kafka-<name>` | Yes |
| `name` | Human-readable display name for the UI | Yes |
| `version` | Always `${project.version}` (resolved from pom.xml) | Yes |
| `description` | Always `${project.description}` (resolved from pom.xml) | Yes |
| `class` | Fully qualified class name of the policy implementation | Yes |
| `type` | Always `policy` | Yes |
| `category` | UI category. Typically `others` | Yes |
| `documentation` | Path to documentation file (relative to project root) | No |
| `schema` | Path to JSON schema for configuration form | No |
| `native_kafka` | Kafka execution phases (see below) | Yes |
| `native_kafka.documentation` | Kafka-specific documentation file | No |
| `native_kafka.schema` | Kafka-specific configuration schema (can differ from HTTP schema) | No |
| `feature` | Enterprise feature flag. Convention: `apim-native-kafka-policy-<name>` | No (enterprise only) |

## Phase Values

| Value | Execution Phases | When to Use |
|-------|-----------------|-------------|
| `INTERACT` | REQUEST, RESPONSE | Protocol-level operations on any Kafka ApiKey (ACL, topic mapping, IP filtering) |
| `PUBLISH` | MESSAGE_REQUEST | Per-message operations on ProduceRequest records (encryption, validation) |
| `SUBSCRIBE` | MESSAGE_RESPONSE | Per-message operations on FetchResponse records (filtering, decryption) |
| `PUBLISH,SUBSCRIBE` | MESSAGE_REQUEST, MESSAGE_RESPONSE | Both message directions (quota, encryption+decryption) |

## Examples from Existing Policies

```properties
# ACL policy (protocol-level)
native_kafka=INTERACT

# Message filtering (subscribe only)
native_kafka=SUBSCRIBE

# Quota (both directions)
native_kafka=PUBLISH,SUBSCRIBE

# Encryption/Decryption (both directions)
native_kafka=PUBLISH,SUBSCRIBE
```

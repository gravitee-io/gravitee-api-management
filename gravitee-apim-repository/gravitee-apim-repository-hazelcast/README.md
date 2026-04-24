# Gravitee APIM Hazelcast Repository

A rate-limit repository (`Scope.RATE_LIMIT`) backed by Hazelcast.

## Design

This plugin has **no** `com.hazelcast.*` compile-time dependency. It stores counters in a `DistributedMap` obtained from `io.gravitee.node.api.cluster.DistributedMapProvider` — an SPI supplied by gravitee-node's Hazelcast provider plugin (`gravitee-node-cluster-plugin-hazelcast-provider`). The provider owns the embedded Hazelcast instance; this plugin just asks it for a map by name.

There is nothing to configure in this plugin. Hazelcast-level settings (discovery, members, CP subsystem, merge policies, ports) live in `hazelcast-cluster.xml` owned by the Hazelcast provider plugin.

## Enabling

```yaml
ratelimit:
  type: hazelcast
```

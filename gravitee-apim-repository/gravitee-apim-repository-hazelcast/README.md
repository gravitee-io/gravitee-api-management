# Gravitee APIM Hazelcast Repository

A rate-limit repository (`Scope.RATE_LIMIT`) backed by Hazelcast.

## Design

This plugin has **no** `com.hazelcast.*` compile-time dependency. It stores counters in a `DistributedMap` obtained from `io.gravitee.node.api.cluster.ClusterManager`, which is supplied by the Hazelcast cluster plugin (`gravitee-node-cluster-plugin-hazelcast`). The gateway therefore runs a single embedded Hazelcast instance — the one owned by `gravitee-node` — and this plugin reuses it.

As a consequence:

- `cluster.type: hazelcast` is required. Running with `cluster.type: standalone` throws `UnsupportedOperationException` at startup, by design (local counters on a single node are not distributed rate limiting; use `ratelimit.type: memory` instead).
- The Hazelcast version shipped by `gravitee-node` is the authoritative one. This plugin has no version to align.
- There is nothing to configure in this plugin. All Hazelcast-level settings (cluster discovery, members, CP subsystem, merge policies, ports) live in `hazelcast-cluster.xml` owned by the cluster plugin.

## Enabling

```yaml
ratelimit:
  type: hazelcast

cluster:
  type: hazelcast
```

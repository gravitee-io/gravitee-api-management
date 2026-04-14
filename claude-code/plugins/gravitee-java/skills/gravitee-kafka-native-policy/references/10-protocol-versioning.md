# Kafka Protocol Versioning

## How It Works

Every Kafka request carries an **API key** (identifies the operation) and an **API version** (identifies the schema). Together they uniquely determine the wire format of the request and response. Clients discover supported versions via `ApiVersionsRequest`, then use `Math.min(clientMax, brokerMax)` to select the version.

The Gravitee gateway sits between client and broker, so it performs **version intersection**: the advertised version range is `max(gatewayMin, brokerMin)` to `min(gatewayMax, brokerMax)`. Any API where `min > max` after intersection is removed entirely.

**This means policy code must handle the version that was negotiated, which can vary per connection.**

## Why This Matters for Policies

Request and response Java classes from the `kafka-clients` library change between versions:

- **Fields appear/disappear**: e.g., `FindCoordinatorRequest.data().key()` exists in v0-v3, replaced by `coordinatorKeys()` in v4+
- **Collections change type**: e.g., `DeleteTopicsRequest.data().topicNames()` (v0-v5, list of strings) vs `topics()` (v6+, `DeleteTopicsTopicCollection`)
- **Encoding changes**: e.g., ProduceRequest v9+ uses varint for topic name length, v3-v8 uses fixed short
- **Authorization semantics change**: e.g., `AddPartitionsToTxnRequest` v4+ checks CLUSTER authorization, v3- checks per-transactional-id
- **Response fields are version-gated**: e.g., `MetadataResponse` only includes `authorizedOperations` in v8+, `ProduceResponse` only includes `nodeEndpoints` in v10+

## Accessing the Version

```java
// In onRequest / onResponse (INTERACT policies)
@Override
public Completable onRequest(KafkaExecutionContext ctx) {
    return Completable.defer(() -> {
        AbstractRequest request = ctx.request().delegate();
        short version = request.version();  // The negotiated version for this request

        // Branch logic based on version
        if (version >= 4) {
            // Use v4+ fields
        } else {
            // Use legacy fields
        }

        return Completable.complete();
    });
}
```

## Patterns from Existing Policies

### Pattern 1: Field Access Varies by Version

From **kafka-topic-mapping** (`DeleteTopicsMappingOperation`):

```java
// v0-v5: topics stored as a flat list of strings
if (request.data().topicNames() != null) {
    request.data().setTopicNames(
        request.data().topicNames().stream()
            .map(topicName -> mapTopic(mappings, topicName))
            .toList()
    );
}

// v6+: topics stored as a typed collection with topicId support
request.data().topics().forEach(topic -> {
    mapTopicEntry(mappings, topic);
});
```

### Pattern 2: Authorization Model Changes by Version

From **kafka-acl** (`AddPartitionsToTxnAuthorizeStrategy`):

```java
final AddPartitionsToTxnRequest request = castRequest(ctx);

if (request.version() >= 4) {
    // v4+ uses CLUSTER-level authorization
    if (!Authorizer.isAuthorizedByResourceTypeAndOperation(
        authorizations,
        Action.create(AclOperation.CLUSTER_ACTION, ResourceType.CLUSTER), ctx)) {
        throw AclException.create(
            Errors.CLUSTER_AUTHORIZATION_FAILED, List.of(),
            request.getErrorResponse(DEFAULT_THROTTLE_MS, Errors.CLUSTER_AUTHORIZATION_FAILED.exception()));
    }
    return;
}

// v3 and below: single transactional id
final String transactionalId = request.data().v3AndBelowTransactionalId();
// ... per-transactional-id authorization
```

### Pattern 3: Single Value vs List by Version

From **kafka-acl** (`FindCoordinatorAuthorizeStrategy`):

```java
final short version = request.version();

// v4+ uses a list of coordinator keys
// v0-v3 uses a single key
List<String> coordinatorKeys = version < 4
    ? List.of(requestData.key())
    : requestData.coordinatorKeys();
```

### Pattern 4: Optional Response Fields by Version

From **kafka-acl** (`MetadataAuthorizeStrategy`):

```java
// Authorized operations support was added in MetadataResponse v8
if (request.version() >= 8) {
    // Cluster authorized operations only in v8-v10
    if (request.version() <= 10) {
        if (request.data().includeClusterAuthorizedOperations()) {
            response.data().setClusterAuthorizedOperations(authorizedOps);
        }
    }
    // Topic authorized operations in v8+
    if (request.data().includeTopicAuthorizedOperations()) {
        topics.forEach(topic -> topic.setTopicAuthorizedOperations(ops));
    }
}
```

## Guidelines for Policy Authors

1. **Always check which versions your target ApiKeys support.** Consult the [Kafka protocol spec](https://kafka.apache.org/41/design/protocol/) for the schema of each version.

2. **Use `request.version()` to branch logic** when the field layout changes between versions. Don't assume the latest version.

3. **Use the `v3AndBelow*()` / `topics()` naming convention** that Kafka's generated classes provide. These accessors are version-specific by design.

4. **Test with multiple versions.** A client connecting with an older protocol version will send structurally different messages than one using the latest version.

5. **Use `request.getErrorResponse(throttleMs, exception)` when possible.** This method generates a version-appropriate error response automatically, so you don't need to handle per-version response construction yourself.

6. **Null-check version-gated fields.** Fields that don't exist in older versions may return `null` or empty collections. Guard accordingly.

7. **For INTERACT policies operating on many ApiKeys**, consider a per-ApiKey strategy/operation class (as kafka-acl and kafka-topic-mapping do). This isolates version-specific logic per operation rather than mixing it all in the main policy class.

## API Keys Reference (Kafka 4.1)

| API Name | Key | Common Use in Policies |
|----------|-----|----------------------|
| Produce | 0 | Message encryption, quota, ACL |
| Fetch | 1 | Message filtering/decryption, quota, ACL |
| ListOffsets | 2 | Topic mapping, ACL |
| Metadata | 3 | Topic mapping, ACL, IP filtering |
| OffsetCommit | 8 | ACL |
| OffsetFetch | 9 | ACL |
| FindCoordinator | 10 | ACL |
| JoinGroup | 11 | ACL |
| Heartbeat | 12 | ACL |
| LeaveGroup | 13 | ACL |
| SyncGroup | 14 | ACL |
| DescribeGroups | 15 | ACL |
| ListGroups | 16 | ACL |
| ApiVersions | 18 | Version negotiation (gateway-internal) |
| CreateTopics | 19 | Topic mapping, ACL |
| DeleteTopics | 20 | Topic mapping, ACL |
| DeleteRecords | 21 | ACL |
| InitProducerId | 22 | ACL |
| AddPartitionsToTxn | 24 | ACL |
| AddOffsetsToTxn | 25 | ACL |
| EndTxn | 26 | ACL |
| TxnOffsetCommit | 28 | ACL |
| DescribeConfigs | 32 | ACL |
| AlterConfigs | 33 | ACL |
| CreatePartitions | 37 | ACL |
| DeleteGroups | 42 | ACL |
| OffsetDelete | 47 | ACL |
| DescribeProducers | 61 | ACL |
| DescribeTransactions | 65 | ACL |
| ListTransactions | 66 | ACL |
| ConsumerGroupHeartbeat | 68 | ACL |
| ConsumerGroupDescribe | 69 | ACL |

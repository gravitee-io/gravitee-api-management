# Error Management in Kafka Native Policies

## How Interruption Works

Kafka native policies reject requests by calling `interruptWith()` on the execution context. This signals the gateway to stop the policy chain and send an error response to the Kafka client.

Under the hood, `interruptWith()` emits a `Completable.error(...)` with a specific exception type that the gateway's reactor catches and converts into a Kafka protocol response.

## Two Interruption Methods

### 1. `ctx.interruptWith(Errors errors)` - Simple Error Code

Use when you only need to return a standard Kafka error code. The gateway constructs an appropriate protocol response automatically.

```java
import org.apache.kafka.common.protocol.Errors;

// Simple rejection
return ctx.interruptWith(Errors.TOPIC_AUTHORIZATION_FAILED);

// Convert a generic exception to the closest Kafka error
return ctx.interruptWith(Errors.forException(someException));
```

**Internally:** wraps into `KafkaInterruptionFailureException` which the reactor translates to a protocol-appropriate error response.

### 2. `ctx.interruptWith(AbstractResponse response)` - Full Protocol Response

Use when you need fine-grained control over the response: per-topic/partition errors, throttle times, or custom response data.

```java
import org.apache.kafka.common.requests.ProduceResponse;
import org.apache.kafka.common.message.ProduceResponseData;

ProduceResponseData data = new ProduceResponseData();
data.setThrottleTimeMs(100);
// ... populate topic/partition-level errors
return ctx.interruptWith(new ProduceResponse(data));
```

**Internally:** wraps into `KafkaInterruptionException` which sends the response as-is to the client.

## When to Use Which

| Scenario | Method | Example |
|----------|--------|---------|
| Simple authorization failure | `interruptWith(Errors)` | `Errors.TOPIC_AUTHORIZATION_FAILED` |
| Generic/unknown error | `interruptWith(Errors)` | `Errors.forException(throwable)` |
| Per-topic/partition errors | `interruptWith(AbstractResponse)` | Different error codes per topic |
| Throttle time in response | `interruptWith(AbstractResponse)` | Include `throttleTimeMs` |
| Request-specific error details | `interruptWith(AbstractResponse)` | Use `request.getErrorResponse(...)` |

## Error Handling Patterns

### Pattern 1: Try/Catch with Custom Exception

Used by **kafka-acl** policy. Best for complex authorization logic with multiple failure modes.

```java
@Override
public Completable onRequest(KafkaExecutionContext ctx) {
    return Completable.defer(() -> {
        try {
            authorize(ctx, configuration.getAuthorizations());
            return Completable.complete();
        } catch (AclException e) {
            ApiLog.debug(ctx, log, e.getMessage(), e);
            // Prefer the detailed response if available, fall back to simple Errors
            if (e.getErrorResponse() != null) {
                return ctx.interruptWith(e.getErrorResponse());
            }
            return ctx.interruptWith(e.getErrors());
        }
    });
}
```

Custom exception class:

```java
@Getter
public class PolicyException extends Exception {

    private final Errors errors;
    private transient AbstractResponse errorResponse;

    public PolicyException(Errors errors, String message) {
        super(message, errors.exception());
        this.errors = errors;
        this.errorResponse = null;
    }

    public PolicyException(AbstractResponse errorResponse, String message) {
        super(message);
        this.errorResponse = errorResponse;
        this.errors = null;
    }
}
```

### Pattern 2: onErrorResumeNext with Exception Mapping

Used by **kafka-topic-mapping** policy. Best for reactive chains where errors propagate naturally.

```java
@Override
public Completable onRequest(KafkaExecutionContext ctx) {
    return Completable
        .fromRunnable(() -> {
            // Business logic that may throw
            doSomething(ctx);
        })
        .onErrorResumeNext(th -> {
            if (th instanceof TopicMappingException ex) {
                log.debug(th.getMessage(), th);
                return ctx.interruptWith(ex.getError());
            }
            // Fallback: map any unknown exception to a Kafka error
            return ctx.interruptWith(Errors.forException(th));
        });
}
```

Custom exception class:

```java
@Getter
public class TopicMappingException extends RuntimeException {
    private final Errors error;

    public TopicMappingException(String message, Errors error) {
        super(message);
        this.error = error;
    }
}
```

### Pattern 3: Direct Interruption (No Exception)

Best for simple policies with straightforward validation.

```java
@Override
public Completable onRequest(KafkaExecutionContext ctx) {
    return Completable.defer(() -> {
        if (!isAllowed(ctx)) {
            return ctx.interruptWith(Errors.TOPIC_AUTHORIZATION_FAILED);
        }
        return Completable.complete();
    });
}
```

### Pattern 4: Throttling (No Interruption)

Used by **kafka-quota** policy. Does not reject the request but slows the client down.

```java
// Pause the network (delays reading next request)
ctx.networkController().pause(Duration.ofMillis(throttlingTime));

// Set throttle time in the response (client-side backoff hint)
ctx.response().delegate().maybeSetThrottleTimeMs(throttlingTime);
 ctx.response().notifyChange();
```

## Constructing Detailed Error Responses

For INTERACT policies that need per-topic or per-partition error details, use the Kafka request's built-in `getErrorResponse()` method:

```java
// Let Kafka build the error response from the request structure
AbstractResponse errorResponse = request.getErrorResponse(
    DEFAULT_THROTTLE_MS,
    Errors.TOPIC_AUTHORIZATION_FAILED.exception()
);
return ctx.interruptWith(errorResponse);
```

For manual response construction (e.g., different errors per topic):

```java
ProduceResponseData responseData = new ProduceResponseData();
responseData.setThrottleTimeMs(0);

ProduceResponseData.TopicProduceResponseCollection topicResponses = new ProduceResponseData.TopicProduceResponseCollection();
for (ProduceRequestData.TopicProduceData topicData : request.data().topicData()) {
    ProduceResponseData.TopicProduceResponse topicResponse = new ProduceResponseData.TopicProduceResponse();
    topicResponse.setName(topicData.name());

    for (ProduceRequestData.PartitionProduceData partition : topicData.partitionData()) {
        ProduceResponseData.PartitionProduceResponse partitionResponse = new ProduceResponseData.PartitionProduceResponse();
        partitionResponse.setIndex(partition.index());
        partitionResponse.setErrorCode(Errors.TOPIC_AUTHORIZATION_FAILED.code());
        partitionResponse.setErrorMessage("Access denied to topic: " + topicData.name());
        topicResponse.partitionResponses().add(partitionResponse);
    }
    topicResponses.add(topicResponse);
}
responseData.setResponses(topicResponses);

return ctx.interruptWith(new ProduceResponse(responseData));
```

## Kafka Protocol Error Codes Reference

All error codes are defined in `org.apache.kafka.common.protocol.Errors` enum. Use `Errors.forException(throwable)` to map generic Java exceptions to the closest Kafka error code.

### Errors Commonly Used in Policies

| Errors Enum Value | Code | Retriable | Typical Use Case |
|---|---|---|---|
| `TOPIC_AUTHORIZATION_FAILED` | 29 | No | Unauthorized access to a topic |
| `GROUP_AUTHORIZATION_FAILED` | 30 | No | Unauthorized group operation |
| `CLUSTER_AUTHORIZATION_FAILED` | 31 | No | Unauthorized cluster-level operation |
| `TRANSACTIONAL_ID_AUTHORIZATION_FAILED` | 53 | No | Unauthorized transactional operation |
| `BROKER_NOT_AVAILABLE` | 8 | No | Generic "service unavailable" rejection |
| `POLICY_VIOLATION` | 44 | No | Request violates a configured policy |
| `INVALID_REQUEST` | 42 | No | Malformed or invalid request |
| `UNKNOWN_SERVER_ERROR` | -1 | No | Fallback for unexpected errors |
| `THROTTLING_QUOTA_EXCEEDED` | 89 | Yes | Quota/rate limit exceeded |
| `INVALID_TOPIC_EXCEPTION` | 17 | No | Invalid topic name or mapping |
| `UNKNOWN_TOPIC_OR_PARTITION` | 3 | Yes | Topic/partition not found |

### Full Error Codes Table (Kafka 4.1)

| Error Name | Code | Retriable | Description |
|---|---|---|---|
| `UNKNOWN_SERVER_ERROR` | -1 | No | Unexpected server error |
| `NONE` | 0 | No | Success (no error) |
| `OFFSET_OUT_OF_RANGE` | 1 | No | Requested offset outside maintained range |
| `CORRUPT_MESSAGE` | 2 | Yes | CRC checksum failure or invalid size |
| `UNKNOWN_TOPIC_OR_PARTITION` | 3 | Yes | Server does not host this topic-partition |
| `INVALID_FETCH_SIZE` | 4 | No | Invalid fetch size |
| `LEADER_NOT_AVAILABLE` | 5 | Yes | Leadership election in progress |
| `NOT_LEADER_OR_FOLLOWER` | 6 | Yes | Broker is not leader/replica |
| `REQUEST_TIMED_OUT` | 7 | Yes | Request timed out |
| `BROKER_NOT_AVAILABLE` | 8 | No | Broker unavailable |
| `REPLICA_NOT_AVAILABLE` | 9 | Yes | Replica unavailable |
| `MESSAGE_TOO_LARGE` | 10 | No | Message exceeds max size |
| `STALE_CONTROLLER_EPOCH` | 11 | No | Controller moved |
| `OFFSET_METADATA_TOO_LARGE` | 12 | No | Offset metadata too large |
| `NETWORK_EXCEPTION` | 13 | Yes | Server disconnected |
| `COORDINATOR_LOAD_IN_PROGRESS` | 14 | Yes | Coordinator loading |
| `COORDINATOR_NOT_AVAILABLE` | 15 | Yes | Coordinator unavailable |
| `NOT_COORDINATOR` | 16 | Yes | Wrong coordinator |
| `INVALID_TOPIC_EXCEPTION` | 17 | No | Invalid topic |
| `RECORD_LIST_TOO_LARGE` | 18 | No | Batch exceeds segment size |
| `NOT_ENOUGH_REPLICAS` | 19 | Yes | Fewer in-sync replicas than required |
| `NOT_ENOUGH_REPLICAS_AFTER_APPEND` | 20 | Yes | Written to fewer replicas than required |
| `INVALID_REQUIRED_ACKS` | 21 | No | Invalid acks value |
| `ILLEGAL_GENERATION` | 22 | No | Invalid group generation id |
| `INCONSISTENT_GROUP_PROTOCOL` | 23 | No | Incompatible group protocol |
| `INVALID_GROUP_ID` | 24 | No | Invalid group id |
| `UNKNOWN_MEMBER_ID` | 25 | No | Unknown group member |
| `INVALID_SESSION_TIMEOUT` | 26 | No | Session timeout out of range |
| `REBALANCE_IN_PROGRESS` | 27 | No | Group rebalancing |
| `INVALID_COMMIT_OFFSET_SIZE` | 28 | No | Invalid commit offset size |
| `TOPIC_AUTHORIZATION_FAILED` | 29 | No | Topic authorization failed |
| `GROUP_AUTHORIZATION_FAILED` | 30 | No | Group authorization failed |
| `CLUSTER_AUTHORIZATION_FAILED` | 31 | No | Cluster authorization failed |
| `INVALID_TIMESTAMP` | 32 | No | Message timestamp out of range |
| `UNSUPPORTED_SASL_MECHANISM` | 33 | No | SASL mechanism not supported |
| `ILLEGAL_SASL_STATE` | 34 | No | Invalid SASL state |
| `UNSUPPORTED_VERSION` | 35 | No | API version not supported |
| `TOPIC_ALREADY_EXISTS` | 36 | No | Topic already exists |
| `INVALID_PARTITIONS` | 37 | No | Partition count below 1 |
| `INVALID_REPLICATION_FACTOR` | 38 | No | Invalid replication factor |
| `INVALID_REPLICA_ASSIGNMENT` | 39 | No | Invalid replica assignment |
| `INVALID_CONFIG` | 40 | No | Invalid configuration |
| `NOT_CONTROLLER` | 41 | Yes | Not the correct controller |
| `INVALID_REQUEST` | 42 | No | Malformed request |
| `UNSUPPORTED_FOR_MESSAGE_FORMAT` | 43 | No | Message format unsupported |
| `POLICY_VIOLATION` | 44 | No | Policy violation |
| `OUT_OF_ORDER_SEQUENCE_NUMBER` | 45 | No | Out of order sequence |
| `DUPLICATE_SEQUENCE_NUMBER` | 46 | No | Duplicate sequence |
| `INVALID_PRODUCER_EPOCH` | 47 | No | Old producer epoch |
| `INVALID_TXN_STATE` | 48 | No | Invalid transaction state |
| `INVALID_PRODUCER_ID_MAPPING` | 49 | No | Invalid producer id |
| `INVALID_TRANSACTION_TIMEOUT` | 50 | No | Transaction timeout too large |
| `CONCURRENT_TRANSACTIONS` | 51 | Yes | Concurrent transaction update |
| `TRANSACTION_COORDINATOR_FENCED` | 52 | No | Coordinator fenced |
| `TRANSACTIONAL_ID_AUTHORIZATION_FAILED` | 53 | No | Transactional id auth failed |
| `SECURITY_DISABLED` | 54 | No | Security disabled |
| `OPERATION_NOT_ATTEMPTED` | 55 | No | Operation not attempted |
| `KAFKA_STORAGE_ERROR` | 56 | Yes | Disk error |
| `LOG_DIR_NOT_FOUND` | 57 | No | Log directory not found |
| `SASL_AUTHENTICATION_FAILED` | 58 | No | SASL auth failed |
| `UNKNOWN_PRODUCER_ID` | 59 | No | Unknown producer metadata |
| `REASSIGNMENT_IN_PROGRESS` | 60 | No | Partition reassignment in progress |
| `DELEGATION_TOKEN_AUTH_DISABLED` | 61 | No | Delegation token disabled |
| `DELEGATION_TOKEN_NOT_FOUND` | 62 | No | Delegation token not found |
| `DELEGATION_TOKEN_OWNER_MISMATCH` | 63 | No | Invalid token owner/renewer |
| `DELEGATION_TOKEN_REQUEST_NOT_ALLOWED` | 64 | No | Token request not allowed |
| `DELEGATION_TOKEN_AUTHORIZATION_FAILED` | 65 | No | Token auth failed |
| `DELEGATION_TOKEN_EXPIRED` | 66 | No | Token expired |
| `INVALID_PRINCIPAL_TYPE` | 67 | No | Unsupported principal type |
| `NON_EMPTY_GROUP` | 68 | No | Group not empty |
| `GROUP_ID_NOT_FOUND` | 69 | No | Group id not found |
| `FETCH_SESSION_ID_NOT_FOUND` | 70 | Yes | Fetch session not found |
| `INVALID_FETCH_SESSION_EPOCH` | 71 | Yes | Invalid fetch session epoch |
| `LISTENER_NOT_FOUND` | 72 | Yes | No matching listener |
| `TOPIC_DELETION_DISABLED` | 73 | No | Topic deletion disabled |
| `FENCED_LEADER_EPOCH` | 74 | Yes | Stale leader epoch |
| `UNKNOWN_LEADER_EPOCH` | 75 | Yes | Future leader epoch |
| `UNSUPPORTED_COMPRESSION_TYPE` | 76 | No | Unsupported compression |
| `STALE_BROKER_EPOCH` | 77 | No | Stale broker epoch |
| `OFFSET_NOT_AVAILABLE` | 78 | Yes | High watermark not caught up |
| `MEMBER_ID_REQUIRED` | 79 | No | Member id required |
| `PREFERRED_LEADER_NOT_AVAILABLE` | 80 | Yes | Preferred leader unavailable |
| `GROUP_MAX_SIZE_REACHED` | 81 | No | Group max size reached |
| `FENCED_INSTANCE_ID` | 82 | No | Static consumer fenced |
| `ELIGIBLE_LEADERS_NOT_AVAILABLE` | 83 | Yes | No eligible leaders |
| `ELECTION_NOT_NEEDED` | 84 | Yes | Election not needed |
| `NO_REASSIGNMENT_IN_PROGRESS` | 85 | No | No reassignment in progress |
| `GROUP_SUBSCRIBED_TO_TOPIC` | 86 | No | Group subscribed to topic |
| `INVALID_RECORD` | 87 | No | Record validation failed |
| `UNSTABLE_OFFSET_COMMIT` | 88 | Yes | Unstable offsets |
| `THROTTLING_QUOTA_EXCEEDED` | 89 | Yes | Quota exceeded |
| `PRODUCER_FENCED` | 90 | No | Producer fenced |
| `RESOURCE_NOT_FOUND` | 91 | No | Resource not found |
| `DUPLICATE_RESOURCE` | 92 | No | Duplicate resource |
| `UNACCEPTABLE_CREDENTIAL` | 93 | No | Unacceptable credential |
| `INCONSISTENT_VOTER_SET` | 94 | No | Inconsistent voter set |
| `INVALID_UPDATE_VERSION` | 95 | No | Invalid update version |
| `FEATURE_UPDATE_FAILED` | 96 | No | Feature update failed |
| `PRINCIPAL_DESERIALIZATION_FAILURE` | 97 | No | Principal deserialization failed |
| `SNAPSHOT_NOT_FOUND` | 98 | No | Snapshot not found |
| `POSITION_OUT_OF_RANGE` | 99 | No | Position out of range |
| `UNKNOWN_TOPIC_ID` | 100 | Yes | Unknown topic ID |
| `DUPLICATE_BROKER_REGISTRATION` | 101 | No | Duplicate broker ID |
| `BROKER_ID_NOT_REGISTERED` | 102 | No | Broker ID not registered |
| `INCONSISTENT_TOPIC_ID` | 103 | Yes | Topic ID mismatch |
| `INCONSISTENT_CLUSTER_ID` | 104 | No | Cluster ID mismatch |
| `TRANSACTIONAL_ID_NOT_FOUND` | 105 | No | Transactional ID not found |
| `FETCH_SESSION_TOPIC_ID_ERROR` | 106 | Yes | Fetch session topic ID inconsistency |

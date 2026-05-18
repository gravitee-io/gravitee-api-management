package io.gravitee.gamma.module.authz.entityimport.model;

import java.time.Instant;

public record ScimConnectorResponse(
    String id,
    String environmentId,
    String name,
    String url,
    boolean importUsers,
    boolean importGroups,
    int intervalSeconds,
    Instant lastSyncAt,
    String lastSyncStatus,
    String lastError,
    int lastUsersSynced,
    int lastGroupsSynced,
    int lastDeleted,
    Instant createdAt,
    Instant updatedAt
) {}

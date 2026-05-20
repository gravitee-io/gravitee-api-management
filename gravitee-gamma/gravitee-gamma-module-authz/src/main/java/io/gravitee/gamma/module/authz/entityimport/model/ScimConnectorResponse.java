package io.gravitee.gamma.module.authz.entityimport.model;

import java.time.Instant;

public record ScimConnectorResponse(
    String id,
    String environmentId,
    String name,
    String url,
    String tokenUrl,
    String clientId,
    boolean importUsers,
    boolean importGroups,
    int intervalSeconds,
    Instant lastSyncAt,
    String lastSyncStatus,
    String lastError,
    Instant accessTokenExpiresAt,
    int lastUsersSynced,
    int lastGroupsSynced,
    int lastDeleted,
    Instant createdAt,
    Instant updatedAt
) {}

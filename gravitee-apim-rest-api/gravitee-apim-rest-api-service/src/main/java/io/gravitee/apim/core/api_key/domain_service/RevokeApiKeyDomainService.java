package io.gravitee.apim.core.api_key.domain_service;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

public interface RevokeApiKeyDomainService {
    Set<ApiKey> revokeAllSubscriptionsApiKeys(
        ExecutionContext executionContext,
        String apiId,
        String subscriptionId,
        AuditActor currentUser
    );
}

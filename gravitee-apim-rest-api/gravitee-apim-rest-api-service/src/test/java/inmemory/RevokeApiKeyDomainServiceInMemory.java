package inmemory;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RevokeApiKeyDomainServiceInMemory implements RevokeApiKeyDomainService {

    Map<String, Set<ApiKey>> storage = new HashMap<>();

    public void initWith(Map<String, Set<ApiKey>> initialValues) {
        initialValues.forEach((key, values) -> storage.put(key, values.stream().map(ApiKey::new).collect(Collectors.toSet())));
    }

    @Override
    public Set<ApiKey> revokeAllSubscriptionsApiKeys(
        ExecutionContext executionContext,
        String apiId,
        String subscriptionId,
        AuditActor currentUser
    ) {
        Date now = new Date();
        return storage
            .get(subscriptionId)
            .stream()
            .peek(key -> {
                key.setRevoked(true);
                key.setRevokedAt(now);
            })
            .collect(Collectors.toSet());
    }

    public Set<ApiKey> getApiKeysBySubscriptionId(String subscriptionId) {
        return storage.get(subscriptionId);
    }

    public Map<String, Set<ApiKey>> getStorage() {
        return storage;
    }
}

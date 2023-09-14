/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package inmemory;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RevokeApiKeyDomainServiceInMemory implements RevokeApiKeyDomainService {

    Map<String, Set<ApiKeyEntity>> storage = new HashMap<>();

    public void initWith(Map<String, Set<ApiKeyEntity>> initialValues) {
        initialValues.forEach((key, values) ->
            storage.put(key, values.stream().map(apiKeyEntity -> apiKeyEntity.toBuilder().build()).collect(Collectors.toSet()))
        );
    }

    @Override
    public Set<ApiKeyEntity> revokeAllSubscriptionsApiKeys(
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

    public Set<ApiKeyEntity> getApiKeysBySubscriptionId(String subscriptionId) {
        return storage.get(subscriptionId);
    }

    public Map<String, Set<ApiKeyEntity>> getStorage() {
        return storage;
    }
}

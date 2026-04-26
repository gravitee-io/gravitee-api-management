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
package io.gravitee.apim.core.api_key.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.crd.ApiKeyCRDSpec;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class ReconcileApiKeysDomainService {

    private final ApiKeyQueryService apiKeyQueryService;
    private final ApiKeyCrudService apiKeyCrudService;
    private final GenerateApiKeyDomainService generateApiKeyDomainService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;

    /**
     * Reconciles the desired API keys from the CRD spec against the actual keys in APIM for a given subscription.
     *
     * <ul>
     *   <li>Key in spec, active in APIM: update expireAt if changed</li>
     *   <li>Key in spec, revoked in APIM: reactivate it</li>
     *   <li>Key in spec, not in APIM: create it</li>
     *   <li>Key not in spec, active in APIM: revoke immediately</li>
     *   <li>Key not in spec, revoked in APIM: no-op</li>
     * </ul>
     */
    public void reconcile(SubscriptionEntity subscription, List<ApiKeyCRDSpec> desiredKeys, AuditInfo auditInfo) {
        if (desiredKeys == null || desiredKeys.isEmpty()) {
            return;
        }

        var existingByKey = apiKeyQueryService
            .findBySubscription(subscription.getId())
            .collect(Collectors.toMap(ApiKeyEntity::getKey, Function.identity(), (a, b) -> a));

        var desiredByKey = desiredKeys.stream().collect(Collectors.toMap(ApiKeyCRDSpec::getKey, Function.identity()));

        for (ApiKeyCRDSpec desired : desiredKeys) {
            var existing = existingByKey.get(desired.getKey());
            if (existing == null) {
                createKey(subscription, desired, auditInfo);
            } else if (existing.isRevoked()) {
                reactivateKey(existing, desired);
            } else {
                updateExpiryIfChanged(existing, desired);
            }
        }

        for (var entry : existingByKey.entrySet()) {
            if (!desiredByKey.containsKey(entry.getKey()) && entry.getValue().canBeRevoked()) {
                log.debug("Revoking API key [{}] not present in desired spec", entry.getKey());
                revokeApiKeyDomainService.revoke(entry.getValue(), auditInfo);
            }
        }
    }

    private void createKey(SubscriptionEntity subscription, ApiKeyCRDSpec desired, AuditInfo auditInfo) {
        log.debug("Creating API key [{}] for subscription [{}]", desired.getKey(), subscription.getId());
        var created = generateApiKeyDomainService.generate(subscription, auditInfo, desired.getKey());
        if (desired.getExpireAt() != null) {
            apiKeyCrudService.update(created.toBuilder().expireAt(desired.getExpireAt()).build());
        }
    }

    private void reactivateKey(ApiKeyEntity existing, ApiKeyCRDSpec desired) {
        log.debug("Reactivating revoked API key [{}]", existing.getKey());
        var reactivated = existing.reactivate();
        if (desired.getExpireAt() != null) {
            reactivated = reactivated.toBuilder().expireAt(desired.getExpireAt()).build();
        }
        apiKeyCrudService.update(reactivated);
    }

    private void updateExpiryIfChanged(ApiKeyEntity existing, ApiKeyCRDSpec desired) {
        if (!Objects.equals(desired.getExpireAt(), existing.getExpireAt())) {
            log.debug("Updating expireAt for API key [{}]", existing.getKey());
            apiKeyCrudService.update(existing.toBuilder().expireAt(desired.getExpireAt()).build());
        }
    }
}

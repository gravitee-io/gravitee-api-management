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
package io.gravitee.apim.core.subscription.domain_service;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;

/**
 * Domain service for creating subscriptions.
 * This service wraps the legacy SubscriptionService to maintain core independence.
 *
 * @author GraviteeSource Team
 */
public interface CreateSubscriptionDomainService {
    /**
     * Create a new subscription.
     *
     * @param auditInfo Audit information
     * @param planId Plan ID
     * @param applicationId Application ID
     * @param requestMessage Optional request message
     * @param customApiKey Optional custom API key
     * @param configuration Optional subscription configuration
     * @param metadata Optional metadata
     * @param apiKeyMode Optional API key mode (SHARED or EXCLUSIVE)
     * @param generalConditionsAccepted Whether general conditions were accepted
     * @param generalConditionsContentRevision General conditions content revision
     * @return The created subscription entity (REST model, will be converted to core by caller)
     */
    io.gravitee.rest.api.model.SubscriptionEntity create(
        AuditInfo auditInfo,
        String planId,
        String applicationId,
        String requestMessage,
        String customApiKey,
        io.gravitee.apim.core.subscription.model.SubscriptionConfiguration configuration,
        java.util.Map<String, String> metadata,
        io.gravitee.rest.api.model.ApiKeyMode apiKeyMode,
        Boolean generalConditionsAccepted,
        io.gravitee.rest.api.model.PageEntity.PageRevisionId generalConditionsContentRevision
    );
}

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

/**
 * Domain service for updating subscriptions.
 * This service wraps the legacy SubscriptionService to maintain core independence.
 *
 * @author GraviteeSource Team
 */
public interface UpdateSubscriptionDomainService {
    /**
     * Update an existing subscription.
     *
     * @param auditInfo Audit information
     * @param subscriptionId Subscription ID
     * @param configuration Optional subscription configuration
     * @param metadata Optional metadata
     * @param startingAt Optional starting date
     * @param endingAt Optional ending date
     */
    void update(
        AuditInfo auditInfo,
        String subscriptionId,
        io.gravitee.rest.api.model.SubscriptionConfigurationEntity configuration,
        java.util.Map<String, String> metadata,
        java.time.ZonedDateTime startingAt,
        java.time.ZonedDateTime endingAt
    );
}

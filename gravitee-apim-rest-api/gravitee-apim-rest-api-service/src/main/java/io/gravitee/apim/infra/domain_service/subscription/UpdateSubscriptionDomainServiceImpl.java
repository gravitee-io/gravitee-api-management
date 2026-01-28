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
package io.gravitee.apim.infra.domain_service.subscription;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.UpdateSubscriptionDomainService;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of UpdateSubscriptionDomainService that wraps SubscriptionService.
 *
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class UpdateSubscriptionDomainServiceImpl implements UpdateSubscriptionDomainService {

    private final SubscriptionService subscriptionService;

    @Override
    public void update(
        AuditInfo auditInfo,
        String subscriptionId,
        io.gravitee.rest.api.model.SubscriptionConfigurationEntity configuration,
        java.util.Map<String, String> metadata,
        java.time.ZonedDateTime startingAt,
        java.time.ZonedDateTime endingAt
    ) {
        ExecutionContext executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());

        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setId(subscriptionId);
        if (configuration != null) {
            updateSubscriptionEntity.setConfiguration(configuration);
        }
        if (metadata != null) {
            updateSubscriptionEntity.setMetadata(metadata);
        }
        if (startingAt != null) {
            updateSubscriptionEntity.setStartingAt(Date.from(startingAt.toInstant()));
        }
        if (endingAt != null) {
            updateSubscriptionEntity.setEndingAt(Date.from(endingAt.toInstant()));
        }

        subscriptionService.update(executionContext, updateSubscriptionEntity);
    }
}

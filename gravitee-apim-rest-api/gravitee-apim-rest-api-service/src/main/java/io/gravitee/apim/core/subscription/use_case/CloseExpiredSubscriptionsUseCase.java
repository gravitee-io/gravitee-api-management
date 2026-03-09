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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UseCase
public class CloseExpiredSubscriptionsUseCase {

    private final SubscriptionQueryService subscriptionQueryService;
    private final EnvironmentCrudService environmentCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiProductCrudService apiProductCrudService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    public CloseExpiredSubscriptionsUseCase(
        SubscriptionQueryService subscriptionQueryService,
        EnvironmentCrudService environmentCrudService,
        ApiCrudService apiCrudService,
        ApiProductCrudService apiProductCrudService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        this.subscriptionQueryService = subscriptionQueryService;
        this.environmentCrudService = environmentCrudService;
        this.apiCrudService = apiCrudService;
        this.apiProductCrudService = apiProductCrudService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
    }

    public Output execute(Input input) {
        var expiredSubscriptions = subscriptionQueryService.findExpiredSubscriptions();
        var environmentIdByApiOrProductId = buildEnvironmentIdMap(expiredSubscriptions);

        var closedSubscriptions = expiredSubscriptions
            .stream()
            .map(subscription -> {
                var environmentId = resolveEnvironmentId(subscription, environmentIdByApiOrProductId);
                var environment = environmentCrudService.get(environmentId);
                var auditInfo = AuditInfo.builder()
                    .organizationId(environment.getOrganizationId())
                    .environmentId(environment.getId())
                    .actor(input.auditActor)
                    .build();

                return closeSubscriptionDomainService.closeSubscription(subscription.getId(), auditInfo);
            })
            .toList();

        return new Output(closedSubscriptions);
    }

    private Map<String, String> buildEnvironmentIdMap(List<SubscriptionEntity> subscriptions) {
        Set<String> apiIds = new HashSet<>();
        Set<String> apiProductIds = new HashSet<>();

        for (var subscription : subscriptions) {
            var referenceId = getEffectiveReferenceId(subscription);
            if (referenceId == null) {
                continue;
            }
            if (subscription.getReferenceType() == SubscriptionReferenceType.API_PRODUCT) {
                apiProductIds.add(referenceId);
            } else {
                apiIds.add(referenceId);
            }
        }

        var environmentIdByApiOrProductId = new HashMap<String, String>();
        if (!apiIds.isEmpty()) {
            apiCrudService
                .findByIds(List.copyOf(apiIds))
                .forEach(api -> environmentIdByApiOrProductId.put(api.getId(), api.getEnvironmentId()));
        }
        if (!apiProductIds.isEmpty()) {
            apiProductCrudService
                .findByIds(apiProductIds)
                .forEach(apiProduct -> environmentIdByApiOrProductId.put(apiProduct.getId(), apiProduct.getEnvironmentId()));
        }
        return environmentIdByApiOrProductId;
    }

    private String getEffectiveReferenceId(SubscriptionEntity subscription) {
        return subscription.getReferenceType() != null && subscription.getReferenceId() != null
            ? subscription.getReferenceId()
            : subscription.getApiId();
    }

    private String resolveEnvironmentId(SubscriptionEntity subscription, Map<String, String> environmentIdByApiOrProductId) {
        // Always derive environment from API/Product
        var apiOrProductId = getEffectiveReferenceId(subscription);
        var environmentId = apiOrProductId != null ? environmentIdByApiOrProductId.get(apiOrProductId) : null;
        if (environmentId == null) {
            throw new IllegalStateException(
                "Subscription %s has no resolvable referenceId/apiId to determine environment".formatted(subscription.getId())
            );
        }
        return environmentId;
    }

    public record Input(AuditActor auditActor) {}

    public record Output(List<SubscriptionEntity> closedSubscriptions) {}
}

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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import java.util.List;
import java.util.stream.Collectors;

@UseCase
public class CloseExpiredSubscriptionsUseCase {

    private final SubscriptionQueryService subscriptionQueryService;
    private final ApiQueryService apiQueryService;
    private final EnvironmentCrudService environmentCrudService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    public CloseExpiredSubscriptionsUseCase(
        SubscriptionQueryService subscriptionQueryService,
        ApiQueryService apiQueryService,
        EnvironmentCrudService environmentCrudService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        this.subscriptionQueryService = subscriptionQueryService;
        this.apiQueryService = apiQueryService;
        this.environmentCrudService = environmentCrudService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
    }

    public Output execute(Input input) {
        var toClose = subscriptionQueryService.findExpiredSubscriptions();

        var apiIds = toClose.stream().map(SubscriptionEntity::getApiId).toList();
        var apis = apiQueryService
            .search(
                ApiSearchCriteria.builder().ids(apiIds).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build()
            )
            .collect(Collectors.toMap(Api::getId, api -> api));

        var closed = toClose
            .stream()
            .map(subscription -> {
                var environment = environmentCrudService.get(apis.get(subscription.getApiId()).getEnvironmentId());

                var auditInfo = AuditInfo.builder()
                    .organizationId(environment.getOrganizationId())
                    .environmentId(environment.getId())
                    .actor(input.auditActor)
                    .build();

                return closeSubscriptionDomainService.closeSubscription(subscription.getId(), auditInfo);
            })
            .toList();

        return new Output(closed);
    }

    public record Input(AuditActor auditActor) {}

    public record Output(List<SubscriptionEntity> closedSubscriptions) {}
}

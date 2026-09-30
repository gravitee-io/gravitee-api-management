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
package io.gravitee.apim.core.ai_workspace.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceMembership;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Accepted API Product subscriptions of the caller's applications.
 * One user maps to one subscription per workspace; if more than one exists, the oldest is kept.
 */
@DomainService
@RequiredArgsConstructor
public class AiWorkspaceMembershipQuery {

    private static final int MEMBERSHIP_PAGE_SIZE = 500;
    private static final Comparator<SubscriptionEntity> OLDEST_FIRST = Comparator.comparing(
        SubscriptionEntity::getCreatedAt,
        Comparator.nullsLast(Comparator.naturalOrder())
    ).thenComparing(SubscriptionEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final SubscriptionSearchQueryService subscriptionSearchQueryService;

    public List<AiWorkspaceMembership> find(ExecutionContext executionContext, Set<String> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return List.of();
        }
        var byProduct = new LinkedHashMap<String, SubscriptionEntity>();
        subscriptions(executionContext, applicationIds)
            .stream()
            .filter(subscription -> subscription.getReferenceId() != null)
            .filter(subscription -> "API_PRODUCT".equals(subscription.getReferenceType()))
            .sorted(OLDEST_FIRST)
            .forEach(subscription -> byProduct.putIfAbsent(subscription.getReferenceId(), subscription));
        return byProduct
            .values()
            .stream()
            .map(subscription ->
                new AiWorkspaceMembership(
                    subscription.getReferenceId(),
                    subscription.getPlan(),
                    subscription.getApplication(),
                    subscription.getId(),
                    subscription.getCreatedAt()
                )
            )
            .toList();
    }

    private List<SubscriptionEntity> subscriptions(ExecutionContext executionContext, Set<String> applicationIds) {
        var criteria = new SubscriptionSearchQueryService.Criteria(
            Set.of(SubscriptionReferenceType.API_PRODUCT),
            null,
            null,
            applicationIds,
            null,
            Set.of(SubscriptionStatus.ACCEPTED),
            null
        );
        List<SubscriptionEntity> subscriptions = new ArrayList<>();
        int pageNumber = 1;
        while (true) {
            Page<SubscriptionEntity> page = subscriptionSearchQueryService.search(
                executionContext,
                criteria,
                new PageableImpl(pageNumber, MEMBERSHIP_PAGE_SIZE)
            );
            if (page == null || page.getContent() == null || page.getContent().isEmpty()) {
                return subscriptions;
            }
            subscriptions.addAll(page.getContent());
            if (subscriptions.size() >= page.getTotalElements() || page.getContent().size() < MEMBERSHIP_PAGE_SIZE) {
                return subscriptions;
            }
            pageNumber++;
        }
    }
}

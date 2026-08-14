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
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionTargetSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchPortalSubscriptionsUseCase {

    private final SubscriptionSearchQueryService subscriptionSearchQueryService;
    private final SubscriptionTargetSearchQueryService subscriptionTargetSearchQueryService;

    public Output execute(Input input) {
        Set<SubscriptionReferenceType> referenceTypes = input.referenceTypes() == null || input.referenceTypes().isEmpty()
            ? Set.of(SubscriptionReferenceType.API)
            : Set.copyOf(input.referenceTypes());
        Set<String> apiIds = copyOrNull(input.apiIds());
        Set<String> apiProductIds = copyOrNull(input.apiProductIds());

        Optional<String> query = Optional.ofNullable(input.query())
            .map(String::trim)
            .filter(value -> !value.isEmpty());
        if (query.isPresent()) {
            var matchingTargetIds = subscriptionTargetSearchQueryService.search(input.executionContext(), query.get(), referenceTypes);
            apiIds = intersect(apiIds, matchingTargetIds.apiIds());
            apiProductIds = intersect(apiProductIds, matchingTargetIds.apiProductIds());
            if (apiIds.isEmpty() && apiProductIds.isEmpty()) {
                int pageNumber = input.pageable() == null ? 0 : input.pageable().getPageNumber();
                return new Output(new Page<>(List.of(), pageNumber, 0, 0));
            }
        }

        var criteria = new SubscriptionSearchQueryService.Criteria(
            referenceTypes,
            apiIds,
            apiProductIds,
            input.applicationIds(),
            null,
            input.statuses(),
            null
        );
        Page<SubscriptionEntity> page = subscriptionSearchQueryService.search(input.executionContext(), criteria, input.pageable());

        return new Output(page.map(this::normalizeLegacyApiReference));
    }

    private Set<String> intersect(Set<String> requestedIds, Set<String> matchingIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return matchingIds;
        }
        Set<String> intersection = new HashSet<>(requestedIds);
        intersection.retainAll(matchingIds);
        return Set.copyOf(intersection);
    }

    private Set<String> copyOrNull(Set<String> values) {
        return values == null || values.isEmpty() ? null : Set.copyOf(values);
    }

    private SubscriptionEntity normalizeLegacyApiReference(SubscriptionEntity subscription) {
        if (subscription.getApi() == null || subscription.getApi().isBlank()) {
            return subscription;
        }
        if (subscription.getReferenceType() != null && !SubscriptionReferenceType.API.name().equals(subscription.getReferenceType())) {
            return subscription;
        }
        return subscription
            .toBuilder()
            .referenceId(subscription.getReferenceId() == null ? subscription.getApi() : subscription.getReferenceId())
            .referenceType(SubscriptionReferenceType.API.name())
            .build();
    }

    public record Input(
        ExecutionContext executionContext,
        Set<String> applicationIds,
        Set<SubscriptionStatus> statuses,
        Set<SubscriptionReferenceType> referenceTypes,
        Set<String> apiIds,
        Set<String> apiProductIds,
        String query,
        Pageable pageable
    ) {}

    public record Output(Page<SubscriptionEntity> page) {}
}

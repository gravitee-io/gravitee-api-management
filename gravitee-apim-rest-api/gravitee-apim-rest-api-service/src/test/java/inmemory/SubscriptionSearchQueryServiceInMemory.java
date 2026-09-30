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

import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SubscriptionSearchQueryServiceInMemory implements SubscriptionSearchQueryService {

    private final List<SubscriptionEntity> storage = new ArrayList<>();

    public void initWith(List<SubscriptionEntity> items) {
        storage.clear();
        if (items != null) {
            storage.addAll(items);
        }
    }

    public void reset() {
        storage.clear();
    }

    @Override
    public Page<SubscriptionEntity> search(
        ExecutionContext executionContext,
        String referenceId,
        SubscriptionReferenceType referenceType,
        Set<String> applicationIds,
        Set<String> planIds,
        Set<SubscriptionStatus> statuses,
        String apiKey,
        Pageable pageable
    ) {
        int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : 10;
        if (storage.isEmpty()) {
            return new Page<>(Collections.emptyList(), pageNumber + 1, pageSize, 0);
        }
        List<SubscriptionEntity> matched = storage
            .stream()
            .filter(subscription -> referenceId == null || referenceId.equals(subscription.getReferenceId()))
            .filter(subscription -> referenceType == null || referenceType.name().equals(subscription.getReferenceType()))
            .filter(
                subscription -> applicationIds == null || applicationIds.isEmpty() || applicationIds.contains(subscription.getApplication())
            )
            .filter(subscription -> planIds == null || planIds.isEmpty() || planIds.contains(subscription.getPlan()))
            .filter(subscription -> statuses == null || statuses.isEmpty() || statuses.contains(subscription.getStatus()))
            .filter(subscription -> apiKey == null || (subscription.getKeys() != null && subscription.getKeys().contains(apiKey)))
            .toList();
        return page(matched, pageable, pageNumber + 1, pageSize);
    }

    @Override
    public Page<SubscriptionEntity> search(ExecutionContext executionContext, Criteria criteria, Pageable pageable) {
        int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
        int pageSize = pageable != null ? pageable.getPageSize() : 10;
        if (storage.isEmpty()) {
            return new Page<>(Collections.emptyList(), pageNumber, pageSize, 0);
        }
        List<SubscriptionEntity> matched = storage
            .stream()
            .filter(subscription -> matches(subscription, criteria))
            .toList();
        return page(matched, pageable, pageNumber, pageSize);
    }

    private static boolean matches(SubscriptionEntity subscription, Criteria criteria) {
        if (criteria == null) {
            return true;
        }
        if (criteria.referenceTypes() != null && !criteria.referenceTypes().isEmpty()) {
            boolean typeMatches = criteria
                .referenceTypes()
                .stream()
                .anyMatch(type -> type.name().equals(subscription.getReferenceType()));
            if (!typeMatches) {
                return false;
            }
        }
        if (
            criteria.applicationIds() != null &&
            !criteria.applicationIds().isEmpty() &&
            !criteria.applicationIds().contains(subscription.getApplication())
        ) {
            return false;
        }
        if (criteria.planIds() != null && !criteria.planIds().isEmpty() && !criteria.planIds().contains(subscription.getPlan())) {
            return false;
        }
        if (criteria.statuses() != null && !criteria.statuses().isEmpty() && !criteria.statuses().contains(subscription.getStatus())) {
            return false;
        }
        if (criteria.apiIds() != null && !criteria.apiIds().isEmpty()) {
            if (!"API".equals(subscription.getReferenceType()) || !criteria.apiIds().contains(subscription.getReferenceId())) {
                return false;
            }
        }
        if (criteria.apiProductIds() != null && !criteria.apiProductIds().isEmpty()) {
            if (
                !"API_PRODUCT".equals(subscription.getReferenceType()) || !criteria.apiProductIds().contains(subscription.getReferenceId())
            ) {
                return false;
            }
        }
        return criteria.apiKey() == null || (subscription.getKeys() != null && subscription.getKeys().contains(criteria.apiKey()));
    }

    private static Page<SubscriptionEntity> page(List<SubscriptionEntity> matched, Pageable pageable, int reportedPage, int pageSize) {
        int size = pageSize < 1 ? matched.size() : pageSize;
        int index = pageable == null ? 0 : Math.max(0, pageable.getPageNumber() - 1) * size;
        if (index >= matched.size()) {
            return new Page<>(List.of(), reportedPage, size, matched.size());
        }
        return new Page<>(matched.subList(index, Math.min(index + size, matched.size())), reportedPage, size, matched.size());
    }
}

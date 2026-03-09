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
package io.gravitee.apim.core.portal_page.domain_service;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationApiVisibilityDomainService {

    private final PortalNavigationItemsQueryService queryService;
    private final ApiPortalMembershipDomainService apiMembershipDomainService;

    /**
     * Enforces portal navigation access control by filtering APIs based on visibility rules and user permissions.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId, @Nullable String userId) {
        return resolveVisibleItemsStream(environmentId, userId).toList();
    }

    public Page<PortalNavigationApi> resolveVisibleItemsPage(String environmentId, @Nullable String userId, Pageable pageable) {
        List<PortalNavigationApi> all = resolveVisibleItemsStream(environmentId, userId).toList();
        int skip = (pageable.getPageNumber() - 1) * pageable.getPageSize();
        int limit = pageable.getPageSize();
        List<PortalNavigationApi> content = all.stream().skip(skip).limit(limit).toList();
        return new Page<>(content, pageable.getPageNumber(), content.size(), all.size());
    }

    private Stream<PortalNavigationApi> resolveVisibleItemsStream(String environmentId, @Nullable String userId) {
        List<PortalNavigationApi> apiItems = queryService
            .search(PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).published(true).root(false).build())
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();

        Set<String> allowedIds = apiItems
            .stream()
            .filter(i -> PortalVisibility.PUBLIC.equals(i.getVisibility()))
            .map(PortalNavigationApi::getApiId)
            .collect(toSet());

        if (userId == null) {
            return apiItems.stream().filter(i -> allowedIds.contains(i.getApiId()));
        }

        Set<String> privateApiIds = apiItems
            .stream()
            .filter(i -> PortalVisibility.PRIVATE.equals(i.getVisibility()))
            .map(PortalNavigationApi::getApiId)
            .collect(toSet());

        allowedIds.addAll(apiMembershipDomainService.filterApiIdsByUserMembership(userId, privateApiIds));
        allowedIds.addAll(apiMembershipDomainService.filterAllowedApiIdsBySubscription(userId, privateApiIds));

        return apiItems.stream().filter(i -> allowedIds.contains(i.getApiId()));
    }

    /**
     * Checks visibility of a single PortalNavigationApi for the given user.
     */
    public boolean isVisibleToUser(PortalNavigationApi item, @Nullable String userId) {
        if (PortalVisibility.PUBLIC.equals(item.getVisibility())) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        Set<String> candidate = Set.of(item.getApiId());
        return (
            !apiMembershipDomainService.filterApiIdsByUserMembership(userId, candidate).isEmpty() ||
            !apiMembershipDomainService.filterAllowedApiIdsBySubscription(userId, candidate).isEmpty()
        );
    }
}

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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationApiVisibilityDomainService {

    private final PortalNavigationItemsQueryService queryService;
    private final ApiPortalMembershipDomainService apiMembershipDomainService;

    /**
     * Resolves visible APIs for unauthenticated portal access where only public APIs should be exposed.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId) {
        return fetchApiItems(environmentId)
            .stream()
            .filter(i -> PortalVisibility.PUBLIC.equals(i.getVisibility()))
            .toList();
    }

    /**
     * Enforces portal navigation access control by filtering APIs based on visibility rules and user permissions.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId, String userId) {
        List<PortalNavigationApi> apiItems = fetchApiItems(environmentId);

        Set<String> publicIds = new HashSet<>();
        Set<String> privateIds = new HashSet<>();
        for (PortalNavigationApi item : apiItems) {
            if (PortalVisibility.PUBLIC.equals(item.getVisibility())) {
                publicIds.add(item.getApiId());
            } else {
                privateIds.add(item.getApiId());
            }
        }

        Set<String> allowedIds = new HashSet<>(publicIds);
        allowedIds.addAll(apiMembershipDomainService.filterApiIdsByUserMembership(userId, privateIds));
        allowedIds.addAll(apiMembershipDomainService.filterAllowedApiIdsBySubscription(userId, privateIds));

        return apiItems
            .stream()
            .filter(i -> allowedIds.contains(i.getApiId()))
            .toList();
    }

    private List<PortalNavigationApi> fetchApiItems(String environmentId) {
        return queryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .root(false)
                    .type(PortalNavigationItemType.API)
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();
    }

    /**
     * Checks if an API is visible in portal navigation for the given user, looking it up by API ID.
     */
    public boolean isApiVisibleToUser(String environmentId, String apiId, @Nullable String userId) {
        return queryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .root(false)
                    .type(PortalNavigationItemType.API)
                    .apiIds(Set.of(apiId))
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .findFirst()
            .map(item -> isVisibleToUser(item, userId))
            .orElse(false);
    }

    /**
     * Checks visibility of a single PortalNavigationApi for the given user.
     */
    public boolean isVisibleToUser(PortalNavigationApi item, String userId) {
        if (PortalVisibility.PUBLIC.equals(item.getVisibility())) {
            return true;
        }
        Set<String> candidate = Set.of(item.getApiId());
        return (
            !apiMembershipDomainService.filterApiIdsByUserMembership(userId, candidate).isEmpty() ||
            !apiMembershipDomainService.filterAllowedApiIdsBySubscription(userId, candidate).isEmpty()
        );
    }
}

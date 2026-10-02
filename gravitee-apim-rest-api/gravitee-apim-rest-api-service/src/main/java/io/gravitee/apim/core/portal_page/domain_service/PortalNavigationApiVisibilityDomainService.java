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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalNavigationApiVisibilityDomainService implements PortalNavigationItemVisibilityService {

    private static final ApiFieldFilter LIGHT_API_FIELD_FILTER = ApiFieldFilter.builder()
        .pictureExcluded(true)
        .definitionExcluded(true)
        .build();

    private final PortalNavigationItemsQueryService queryService;
    private final ApiPortalMembershipDomainService apiMembershipDomainService;
    private final ApiQueryService apiQueryService;

    @Override
    public boolean appliesTo(PortalNavigationItem item) {
        return item instanceof PortalNavigationApi;
    }

    @Override
    public Predicate<PortalNavigationItem> prepareVisibilityPredicate(
        String environmentId,
        PortalNavigationItemViewerContext viewerContext
    ) {
        if (!viewerContext.isPortalMode()) {
            return item -> true;
        }
        Map<String, Boolean> publishedByApiId = new HashMap<>();
        return item -> {
            PortalNavigationApi apiItem = (PortalNavigationApi) item;
            if (!publishedByApiId.containsKey(apiItem.getApiId())) {
                // Resolve the lifecycle of every API of the navigation at once, rather than one query per item.
                Set<String> candidateApiIds = apiIdsOf(fetchApiItems(environmentId, null));
                candidateApiIds.add(apiItem.getApiId());
                candidateApiIds.removeAll(publishedByApiId.keySet());
                Set<String> publishedApiIds = filterPublishedApiIds(environmentId, candidateApiIds);
                candidateApiIds.forEach(apiId -> publishedByApiId.put(apiId, publishedApiIds.contains(apiId)));
            }
            return publishedByApiId.get(apiItem.getApiId()) && !isApiItemHidden(apiItem, viewerContext);
        };
    }

    /**
     * Resolves visible APIs for unauthenticated portal access where only public APIs should be exposed.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId) {
        return resolveVisiblePublicItems(environmentId, null);
    }

    /**
     * Resolves visible APIs for unauthenticated portal access, restricted to a single category, where only
     * public APIs should be exposed. Unlike {@link #resolveVisibleItems(String, String, String)}, this performs
     * no membership/subscription lookups since there is no authenticated user to check access for.
     */
    public List<PortalNavigationApi> resolveVisiblePublicItems(String environmentId, @Nullable String categoryId) {
        return fetchItemsOfPublishedApis(environmentId, categoryId)
            .stream()
            .filter(i -> PortalVisibility.PUBLIC.equals(i.getVisibility()))
            .toList();
    }

    /**
     * Enforces portal navigation access control by filtering APIs based on visibility rules and user permissions.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId, String userId) {
        return resolveVisibleItems(environmentId, userId, null);
    }

    /**
     * Enforces portal navigation access control by filtering APIs based on visibility rules and user permissions,
     * optionally restricted to a single category.
     */
    public List<PortalNavigationApi> resolveVisibleItems(String environmentId, String userId, @Nullable String categoryId) {
        List<PortalNavigationApi> apiItems = fetchItemsOfPublishedApis(environmentId, categoryId);

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

    /**
     * Fetches the published API navigation items whose API is published too: like the classic portal, the
     * portal never exposes an API whose lifecycle state is not {@code PUBLISHED}, whatever the viewer.
     */
    private List<PortalNavigationApi> fetchItemsOfPublishedApis(String environmentId, @Nullable String categoryId) {
        List<PortalNavigationApi> apiItems = fetchApiItems(environmentId, categoryId);
        Set<String> publishedApiIds = filterPublishedApiIds(environmentId, apiIdsOf(apiItems));
        return apiItems
            .stream()
            .filter(item -> publishedApiIds.contains(item.getApiId()))
            .toList();
    }

    private Set<String> filterPublishedApiIds(String environmentId, Set<String> apiIds) {
        if (apiIds.isEmpty()) {
            return Set.of();
        }
        return apiQueryService
            .search(
                ApiSearchCriteria.builder()
                    .environmentId(environmentId)
                    .ids(List.copyOf(apiIds))
                    .lifecycleStates(List.of(Api.ApiLifecycleState.PUBLISHED))
                    .build(),
                null,
                LIGHT_API_FIELD_FILTER
            )
            .map(Api::getId)
            .collect(Collectors.toSet());
    }

    private boolean isApiPublished(String environmentId, String apiId) {
        return filterPublishedApiIds(environmentId, Set.of(apiId)).contains(apiId);
    }

    private static Set<String> apiIdsOf(List<PortalNavigationApi> apiItems) {
        return apiItems.stream().map(PortalNavigationApi::getApiId).collect(Collectors.toCollection(HashSet::new));
    }

    private List<PortalNavigationApi> fetchApiItems(String environmentId, @Nullable String categoryId) {
        PortalCategoryId parsedCategoryId = categoryId != null ? PortalCategoryId.of(categoryId) : null;
        return queryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .root(false)
                    .type(PortalNavigationItemType.API)
                    .categoryId(parsedCategoryId)
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();
    }

    /**
     * Checks if an API is visible in portal navigation for the given user, looking it up by API ID.
     * An API that is not published is never visible.
     */
    public boolean isApiVisibleToUser(String environmentId, String apiId, @Nullable String userId) {
        if (!isApiPublished(environmentId, apiId)) {
            return false;
        }
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

    /**
     * Returns {@code true} when the given API navigation item must be hidden from the viewer,
     * applying portal visibility (PUBLIC / PRIVATE) and, for authenticated users, API
     * membership and subscription-based access rules. Mirrors the logic used by the catalog
     * endpoint so navigation and catalog expose the same set of APIs.
     */
    public boolean isApiItemHidden(PortalNavigationApi item, PortalNavigationItemViewerContext viewerContext) {
        if (!viewerContext.isPortalMode()) {
            return false;
        }
        if (PortalVisibility.PUBLIC.equals(item.getVisibility())) {
            return false;
        }
        if (!viewerContext.isAuthenticated()) {
            return true;
        }
        return viewerContext
            .userId()
            .map(uid -> !isVisibleToUser(item, uid))
            .orElse(true);
    }

    /**
     * Walks the parent chain of the given item and returns {@code true} if any ancestor is a
     * {@link PortalNavigationApi} hidden from the viewer, or whose API is not published. Used to
     * prevent direct access (by id) to descendants of an API navigation item the viewer cannot see.
     */
    public boolean hasHiddenApiAncestor(String environmentId, PortalNavigationItem item, PortalNavigationItemViewerContext viewerContext) {
        if (!viewerContext.isPortalMode()) {
            return false;
        }
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null) {
            current = queryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
            if (
                current instanceof PortalNavigationApi apiAncestor &&
                (isApiItemHidden(apiAncestor, viewerContext) || !isApiPublished(environmentId, apiAncestor.getApiId()))
            ) {
                return true;
            }
        }
        return false;
    }
}

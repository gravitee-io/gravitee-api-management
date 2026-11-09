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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.PortalReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalSummaryDomainService {

    private final PortalUrlDomainService portalUrlDomainService;
    private final ThemeQueryService themeQueryService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    /** Summaries of {@code portals}, all of {@code environmentId}, in the same order. */
    public List<PortalSummary> summarize(String environmentId, List<Portal> portals) {
        if (portals.isEmpty()) {
            return List.of();
        }
        // Until PORTAL-319 gives each portal its own theme, every portal shows the environment's next-gen theme logo.
        var hasLogo = enabledNextGenThemeHasLogo(environmentId);
        var publishedApiIdsByPortal = publishedApiIdsByPortal(environmentId);
        return portals
            .stream()
            .map(portal ->
                new PortalSummary(
                    portal,
                    portalUrlDomainService.resolveUrl(portal).orElse(null),
                    hasLogo,
                    publishedApiIdsByPortal.getOrDefault(portal.getId(), Set.of()).size()
                )
            )
            .toList();
    }

    private boolean enabledNextGenThemeHasLogo(String environmentId) {
        return themeQueryService
            .findByThemeTypeAndEnvironmentId(ThemeType.PORTAL_NEXT, environmentId)
            .stream()
            .filter(Theme::isEnabled)
            .anyMatch(theme -> theme.getLogo() != null && !theme.getLogo().isBlank());
    }

    private Map<PortalId, Set<String>> publishedApiIdsByPortal(String environmentId) {
        var items = portalNavigationItemsQueryService.search(
            PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).build()
        );
        var itemsById = items
            .stream()
            .collect(Collectors.toMap(PortalNavigationItem::getId, Function.identity(), (first, ignored) -> first));
        return items
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .filter(api -> api.getReference() instanceof PortalReference)
            .filter(api -> isPublishedWithAncestors(api, itemsById))
            .collect(
                Collectors.groupingBy(
                    api -> ((PortalReference) api.getReference()).portalId(),
                    Collectors.mapping(PortalNavigationApi::getApiId, Collectors.toSet())
                )
            );
    }

    private static boolean isPublishedWithAncestors(
        PortalNavigationItem item,
        Map<PortalNavigationItemId, PortalNavigationItem> itemsById
    ) {
        Set<PortalNavigationItemId> visited = new HashSet<>();
        var current = item;
        while (current != null && visited.add(current.getId())) {
            if (!Boolean.TRUE.equals(current.getPublished())) {
                return false;
            }
            current = current.getParentId() == null ? null : itemsById.get(current.getParentId());
        }
        return true;
    }
}

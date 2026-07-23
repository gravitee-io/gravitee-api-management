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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class PortalNavigationItemVisibilityEvaluator {

    private final String environmentId;
    private final PortalNavigationItemsQueryService queryService;
    private final List<PreparedVisibilityService> visibilityServices;

    public PortalNavigationItemVisibilityEvaluator(
        String environmentId,
        PortalNavigationItemViewerContext viewerContext,
        PortalNavigationItemsQueryService queryService,
        List<PortalNavigationItemVisibilityService> visibilityServices
    ) {
        this.environmentId = environmentId;
        this.queryService = queryService;
        this.visibilityServices = visibilityServices
            .stream()
            .map(service -> new PreparedVisibilityService(service, service.prepareVisibilityPredicate(environmentId, viewerContext)))
            .toList();
    }

    public boolean isVisible(PortalNavigationItem item) {
        return visibilityServices
            .stream()
            .filter(service -> service.appliesTo(item))
            .findFirst()
            .map(service -> service.isVisible(item))
            .orElse(true);
    }

    public boolean hasHiddenAncestor(PortalNavigationItem item) {
        Set<PortalNavigationItemId> visited = new HashSet<>();
        PortalNavigationItem current = item;
        while (current != null && current.getParentId() != null && visited.add(current.getId())) {
            current = queryService.findByIdAndEnvironmentId(environmentId, current.getParentId());
            if (current != null && !isVisible(current)) {
                return true;
            }
        }
        return false;
    }

    private record PreparedVisibilityService(
        PortalNavigationItemVisibilityService service,
        Predicate<PortalNavigationItem> visibilityPredicate
    ) {
        private boolean appliesTo(PortalNavigationItem item) {
            return service.appliesTo(item);
        }

        private boolean isVisible(PortalNavigationItem item) {
            return visibilityPredicate.test(item);
        }
    }
}

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
import io.gravitee.apim.core.agent.exception.PortalAgentNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalAgentAccessDomainService {

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;

    public PortalNavigationAgent findAccessible(String environmentId, String agentId, PortalNavigationItemViewerContext viewerContext) {
        Set<String> accessibleAgentApiIds = apiVisibilityDomainService.resolveAccessibleAgentApiIds(environmentId, viewerContext);

        return portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .type(PortalNavigationItemType.AGENT)
                    .agentIds(Set.of(agentId))
                    .build()
            )
            .stream()
            .filter(PortalNavigationAgent.class::isInstance)
            .map(PortalNavigationAgent.class::cast)
            .filter(item -> !viewerContext.shouldNotShow(item))
            .filter(item -> !apiVisibilityDomainService.isAgentItemHidden(item, viewerContext, accessibleAgentApiIds))
            .filter(item -> !apiVisibilityDomainService.hasHiddenApiAncestor(environmentId, item, viewerContext))
            .findFirst()
            .orElseThrow(() -> new PortalAgentNotFoundException(agentId));
    }
}

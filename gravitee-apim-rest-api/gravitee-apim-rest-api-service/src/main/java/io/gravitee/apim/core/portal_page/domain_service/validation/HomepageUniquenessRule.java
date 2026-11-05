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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.HomepageAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.PortalReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * For HOMEPAGE area, ensures no top-level item already exists in that area, ignoring ids declared in
 * {@link CreateValidationContext#itemIdsBeingReplaced()}.
 */
@RequiredArgsConstructor
public class HomepageUniquenessRule implements CreatePortalNavigationItemValidationRule {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getArea() == PortalArea.HOMEPAGE;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        var existingHomepage = homepagesSharedWith(item.getReference(), environmentId)
            .stream()
            .filter(existing -> !ctx.itemIdsBeingReplaced().contains(existing.getId()))
            .toList();
        if (!existingHomepage.isEmpty()) {
            throw new HomepageAlreadyExistsException();
        }
    }

    /**
     * Rows written before PORTAL-342 are on the zero UUID, and a console create is validated before its portal is
     * resolved: the zero UUID and a portal therefore share one homepage. PORTAL-345 removes the zero UUID and this
     * bridge with it.
     */
    private List<PortalNavigationItem> homepagesSharedWith(NavigationItemReference reference, String environmentId) {
        if (!(reference instanceof PortalReference portal)) {
            return navigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
                environmentId,
                PortalArea.HOMEPAGE,
                reference
            );
        }
        return navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, PortalArea.HOMEPAGE)
            .stream()
            .filter(
                existing ->
                    existing.getReference() instanceof PortalReference other &&
                    (other.equals(portal) || other.equals(PortalReference.DEFAULT) || portal.equals(PortalReference.DEFAULT))
            )
            .toList();
    }
}

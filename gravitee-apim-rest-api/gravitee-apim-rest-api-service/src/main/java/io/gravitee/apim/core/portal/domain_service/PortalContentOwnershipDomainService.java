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
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_listing.crud_service.PortalListingCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Gives a real portal the navigation items and listings of its environment that name no existing portal:
 * the zero UUID of content older than portals, or a portal since deleted. Only the reference changes.
 */
@DomainService
@RequiredArgsConstructor
public class PortalContentOwnershipDomainService {

    public record Moved(long navigationItems, long listings) {
        public boolean isEmpty() {
            return navigationItems == 0 && listings == 0;
        }
    }

    private final PortalCrudService portalCrudService;
    private final PortalNavigationItemCrudService portalNavigationItemCrudService;
    private final PortalListingCrudService portalListingCrudService;

    /**
     * The portal references found in an environment. They are read before its portals: a portal created
     * after that read has no row among them, so its content is never taken for orphan content.
     */
    private record References(Set<PortalId> navigationItems, Set<PortalId> listings) {}

    /** Moves the orphan content of the portal's environment onto {@code portal}, which must already be stored. */
    public Moved moveOrphansTo(Portal portal) {
        var references = referencesOf(portal.getEnvironmentId());
        return moveOrphansTo(portal, references, portalCrudService.findByEnvironmentId(portal.getEnvironmentId()));
    }

    /**
     * Moves the orphan content of the environment onto its default portal, or else onto its only portal.
     *
     * @return what was moved, empty when the environment has no portal, or several and none is the default one
     */
    public Optional<Moved> moveOrphans(String environmentId) {
        var references = referencesOf(environmentId);
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        return targetAmong(portals).map(target -> moveOrphansTo(target, references, portals));
    }

    private References referencesOf(String environmentId) {
        return new References(
            portalNavigationItemCrudService.findReferencedPortalIds(environmentId),
            portalListingCrudService.findReferencedPortalIds(environmentId)
        );
    }

    private static Optional<Portal> targetAmong(List<Portal> portals) {
        return portals
            .stream()
            .filter(Portal::isDefault)
            .findFirst()
            .or(() -> portals.size() == 1 ? Optional.of(portals.getFirst()) : Optional.empty());
    }

    private Moved moveOrphansTo(Portal target, References references, List<Portal> portalsOfEnvironment) {
        var environmentId = target.getEnvironmentId();
        var existingPortalIds = portalsOfEnvironment.stream().map(Portal::getId).collect(Collectors.toSet());
        var deadInNavigation = without(references.navigationItems(), existingPortalIds);
        var deadInListings = without(references.listings(), existingPortalIds);
        return new Moved(
            deadInNavigation.isEmpty()
                ? 0
                : portalNavigationItemCrudService.updatePortalReferences(environmentId, deadInNavigation, target.getId()),
            deadInListings.isEmpty() ? 0 : portalListingCrudService.updatePortalIds(environmentId, deadInListings, target.getId())
        );
    }

    private static Set<PortalId> without(Set<PortalId> referenced, Set<PortalId> existing) {
        return referenced
            .stream()
            .filter(portalId -> !existing.contains(portalId))
            .collect(Collectors.toSet());
    }
}

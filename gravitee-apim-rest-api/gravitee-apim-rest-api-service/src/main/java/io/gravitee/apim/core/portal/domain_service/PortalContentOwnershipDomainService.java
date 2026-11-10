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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Gives a real portal the navigation items of its environment that name no existing portal: the zero UUID of
 * content older than portals, or a portal since deleted. Only the reference changes. Content managed by the
 * Automation API stays where it is, items and listings alike: their ids derive from their portal's hrid, so
 * GKO and Terraform could no longer delete them once moved.
 */
@DomainService
@RequiredArgsConstructor
public class PortalContentOwnershipDomainService {

    public record Moved(long navigationItems) {
        public boolean isEmpty() {
            return navigationItems == 0;
        }
    }

    private final PortalCrudService portalCrudService;
    private final PortalNavigationItemCrudService portalNavigationItemCrudService;

    /** Moves the orphan content of the portal's environment onto {@code portal}, which must already be stored. */
    public Moved moveOrphansTo(Portal portal) {
        var references = portalNavigationItemCrudService.findReferencedPortalIds(portal.getEnvironmentId());
        return moveOrphansTo(portal, references, portalCrudService.findByEnvironmentId(portal.getEnvironmentId()));
    }

    /**
     * Moves the orphan content of the environment onto its default portal, or else onto its only portal.
     *
     * @return what was moved, empty when the environment has no portal, or several and none is the default one
     */
    public Optional<Moved> moveOrphans(String environmentId) {
        var references = portalNavigationItemCrudService.findReferencedPortalIds(environmentId);
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        return targetAmong(portals).map(target -> moveOrphansTo(target, references, portals));
    }

    private static Optional<Portal> targetAmong(List<Portal> portals) {
        return portals
            .stream()
            .filter(Portal::isDefault)
            .findFirst()
            .or(() -> portals.size() == 1 ? Optional.of(portals.getFirst()) : Optional.empty());
    }

    /**
     * {@code references} are read before {@code portalsOfEnvironment}: a portal created after that read has no row
     * among them, so its content is never taken for orphan content.
     */
    private Moved moveOrphansTo(Portal target, Set<PortalId> references, List<Portal> portalsOfEnvironment) {
        var existingPortalIds = portalsOfEnvironment.stream().map(Portal::getId).collect(Collectors.toSet());
        var dead = without(references, existingPortalIds);
        return new Moved(
            dead.isEmpty() ? 0 : portalNavigationItemCrudService.updatePortalReferences(target.getEnvironmentId(), dead, target.getId())
        );
    }

    private static Set<PortalId> without(Set<PortalId> referenced, Set<PortalId> existing) {
        return referenced
            .stream()
            .filter(portalId -> !existing.contains(portalId))
            .collect(Collectors.toSet());
    }
}

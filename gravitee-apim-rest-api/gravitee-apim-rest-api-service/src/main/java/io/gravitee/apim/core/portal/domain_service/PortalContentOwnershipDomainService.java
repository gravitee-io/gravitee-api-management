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
 * content older than portals, or a portal since deleted. Only the reference changes. Console items go to the
 * target portal. An item the Automation API synced goes only to the portal it was synced for, when that portal
 * still exists, and listings never move: the Automation API derives their ids from their portal's hrid, so on
 * another portal GKO and Terraform could no longer delete them.
 */
@DomainService
@RequiredArgsConstructor
public class PortalContentOwnershipDomainService {

    /**
     * @param navigationItems the items moved
     * @param targetMissing true when the environment has no portal, or several and none is the default one: its
     *     console items had nowhere to go and stayed where they are
     */
    public record Moved(long navigationItems, boolean targetMissing) {
        public Moved(long navigationItems) {
            this(navigationItems, false);
        }

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

    /** Moves the orphan console content of the environment onto its default portal, or else onto its only portal. */
    public Moved moveOrphans(String environmentId) {
        var references = portalNavigationItemCrudService.findReferencedPortalIds(environmentId);
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        var target = targetAmong(portals);
        return new Moved(moveOrphans(environmentId, target, references, portals), target.isEmpty());
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
        return new Moved(moveOrphans(target.getEnvironmentId(), Optional.of(target), references, portalsOfEnvironment));
    }

    /** An Automation API item names its own portal, so it moves whether or not console items have a target. */
    private long moveOrphans(String environmentId, Optional<Portal> target, Set<PortalId> references, List<Portal> portalsOfEnvironment) {
        var existingPortalIds = portalsOfEnvironment.stream().map(Portal::getId).collect(Collectors.toSet());
        var dead = without(references, existingPortalIds);
        if (dead.isEmpty()) {
            return 0;
        }
        var consoleItems = target
            .map(portal -> portalNavigationItemCrudService.updatePortalReferences(environmentId, dead, portal.getId()))
            .orElse(0L);
        var automationItems = portalsOfEnvironment
            .stream()
            .mapToLong(portal -> portalNavigationItemCrudService.updateAutomationPortalReferences(environmentId, dead, portal.getId()))
            .sum();
        return consoleItems + automationItems;
    }

    private static Set<PortalId> without(Set<PortalId> referenced, Set<PortalId> existing) {
        return referenced
            .stream()
            .filter(portalId -> !existing.contains(portalId))
            .collect(Collectors.toSet());
    }
}

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
import io.gravitee.apim.core.portal.exception.DefaultPortalNotFoundException;
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.apim.core.portal.model.Portal;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
@CustomLog
public class DefaultPortalDomainService {

    static final String DEFAULT_PORTAL_NAME = "Developer Portal";

    private final PortalCrudService portalCrudService;
    private final PortalAddressDomainService portalAddressDomainService;
    private final PortalContentOwnershipDomainService portalContentOwnershipDomainService;

    /**
     * Creates the environment's default portal when it has no portal at all; an existing default portal is
     * returned whatever other portals the environment holds. A portal the environment already has is never
     * marked default: one created by the Automation API stays as it is. The portal is
     * created at its environment's shared access point only: while the portal URL cannot be resolved (an invalid portal URL
     * setting), creation is postponed to a later call. On a multi-tenant installation, it is created without an access point.
     * A portal created here receives the environment's content that names no existing portal.
     *
     * @return the default portal, or empty when the environment's only portal is not the default one or
     *     its creation is postponed
     * @throws SeveralPortalsInEnvironmentException when the environment has several portals and no default one
     */
    public Optional<Portal> ensureDefault(String organizationId, String environmentId) {
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        var existingDefault = portals.stream().filter(Portal::isDefault).findFirst();
        if (existingDefault.isPresent()) {
            return existingDefault;
        }
        if (portals.size() > 1) {
            throw severalPortals(environmentId, portals);
        }
        if (portals.isEmpty()) {
            var accessPointId = portalAddressDomainService.sharedAccessPoint(environmentId);
            if (accessPointId.isEmpty() && portalAddressDomainService.assignsAccessPoints()) {
                log.warn(
                    "Default portal of environment [{}] not created yet: its portal URL is not set or is not an absolute http(s) URL without credentials",
                    environmentId
                );
                return Optional.empty();
            }
            var created = portalCrudService.create(
                Portal.createDefault(environmentId, organizationId, DEFAULT_PORTAL_NAME, accessPointId.orElse(null))
            );
            log.info(
                "Default portal [{}] created for environment [{}] at access point [{}]",
                created.getId(),
                environmentId,
                created.getAccessPointId()
            );
            var moved = portalContentOwnershipDomainService.moveOrphansTo(created);
            if (!moved.isEmpty()) {
                log.info(
                    "Moved {} navigation item(s) of environment [{}] onto its new default portal [{}]",
                    moved.navigationItems(),
                    environmentId,
                    created.getId()
                );
            }
            return Optional.of(created);
        }
        var portal = portals.getFirst();
        log.debug(
            "Environment [{}] has a single portal [{}] that is not the default one: no default portal created",
            environmentId,
            portal.getId()
        );
        return Optional.empty();
    }

    public Optional<Portal> findDefault(String environmentId) {
        return portalCrudService.findDefaultByEnvironmentId(environmentId);
    }

    /** @return the environment's default portal, else its only portal: an environment managed by automation has no default one */
    public Optional<Portal> findDefaultOrOnly(String environmentId) {
        return Portal.defaultOrOnly(portalCrudService.findByEnvironmentId(environmentId));
    }

    public Portal getDefault(String environmentId) {
        return findDefault(environmentId).orElseThrow(() -> new DefaultPortalNotFoundException(environmentId));
    }

    public Portal getDefaultOrOnlyForLegacyNavigation(String environmentId) {
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        if (portals.stream().filter(Portal::isDefault).count() > 1) {
            throw new DefaultPortalNotFoundException(environmentId);
        }
        return Portal.defaultOrOnly(portals).orElseThrow(() -> new DefaultPortalNotFoundException(environmentId));
    }

    /** @throws SeveralPortalsInEnvironmentException when the environment has several portals */
    public void checkSinglePortal(String environmentId) {
        findAtMostOnePortal(environmentId);
    }

    /**
     * Links the environment's portals that have no access point, to the access point the environment's portals share. A
     * portal has none when its portal URL could not be resolved when it was stored, or when its access point was released
     * while it was being linked to it. A portal at an existing access point keeps it. Links nothing on a multi-tenant
     * installation.
     *
     * @return the number of portals linked
     */
    public int linkUnlinkedPortals(String environmentId) {
        if (!portalAddressDomainService.assignsAccessPoints()) {
            return 0;
        }
        var accessPointIds = portalAddressDomainService.accessPointIds(environmentId);
        var unlinked = portalCrudService
            .findByEnvironmentId(environmentId)
            .stream()
            .filter(portal -> portal.getAccessPointId() == null || !accessPointIds.contains(portal.getAccessPointId()))
            .toList();
        if (unlinked.isEmpty()) {
            return 0;
        }
        var accessPointId = portalAddressDomainService.sharedAccessPoint(environmentId);
        if (accessPointId.isEmpty()) {
            log.warn(
                "Portal URL of environment [{}] is not set or is not an absolute http(s) URL: its {} portal(s) stay without an access point",
                environmentId,
                unlinked.size()
            );
            return 0;
        }
        unlinked.forEach(portal -> {
            portal.linkAccessPoint(accessPointId.get());
            portalCrudService.update(portal);
        });
        return unlinked.size();
    }

    private List<Portal> findAtMostOnePortal(String environmentId) {
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        if (portals.size() > 1) {
            throw severalPortals(environmentId, portals);
        }
        return portals;
    }

    private static SeveralPortalsInEnvironmentException severalPortals(String environmentId, List<Portal> portals) {
        return new SeveralPortalsInEnvironmentException(
            environmentId,
            portals
                .stream()
                .map(portal -> "[%s] \"%s\"".formatted(portal.getId(), portal.getName()))
                .toList()
        );
    }
}

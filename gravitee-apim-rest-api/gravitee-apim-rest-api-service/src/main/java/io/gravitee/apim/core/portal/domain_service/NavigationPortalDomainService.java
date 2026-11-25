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
import io.gravitee.apim.core.portal.exception.NavigationPortalNotResolvedException;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the default portal for unnamed navigation writes and validates portals for publication operations.
 * Legacy homepage and navigation editor requests can also target the environment's only non-default portal.
 */
@DomainService
@RequiredArgsConstructor
public class NavigationPortalDomainService {

    private final DefaultPortalDomainService defaultPortalDomainService;
    private final PortalCrudService portalCrudService;

    /**
     * @return the environment's default portal, created when the environment has no portal and its URL resolves
     * @throws NavigationPortalNotResolvedException when the environment's only portal comes from the Automation
     *     API, while its default portal is postponed, or when it has several portals
     */
    public Portal resolve(String organizationId, String environmentId) {
        try {
            return defaultPortalDomainService
                .ensureDefault(organizationId, environmentId)
                .orElseThrow(() ->
                    portalCrudService.findByEnvironmentId(environmentId).isEmpty()
                        ? NavigationPortalNotResolvedException.defaultPortalPostponed(environmentId)
                        : NavigationPortalNotResolvedException.managedByAutomation(environmentId)
                );
        } catch (SeveralPortalsInEnvironmentException e) {
            throw NavigationPortalNotResolvedException.severalPortals(environmentId, e);
        }
    }

    public NavigationItemReference.PortalReference resolveReference(String organizationId, String environmentId) {
        return new NavigationItemReference.PortalReference(resolve(organizationId, environmentId).getId());
    }

    /**
     * @throws PortalNotFoundException when the portal is not a portal of the environment
     * @throws NavigationPortalNotResolvedException when the console does not manage the portal
     */
    public NavigationItemReference.PortalReference requireReference(String environmentId, PortalId portalId) {
        var portal = portalCrudService
            .findByIdAndEnvironmentId(portalId, environmentId)
            .orElseThrow(() -> new PortalNotFoundException(portalId.toString()));
        if (!isManagedByConsole(portal)) {
            throw NavigationPortalNotResolvedException.automationPortal(environmentId);
        }
        return new NavigationItemReference.PortalReference(portal.getId());
    }

    /** The environment's portals whose navigation the console manages, in the order they are offered in. */
    public List<Portal> findManagedByConsole(String environmentId) {
        return portalCrudService
            .findByEnvironmentId(environmentId)
            .stream()
            .filter(this::isManagedByConsole)
            .sorted(Portal.DEFAULT_FIRST_THEN_BY_NAME)
            .toList();
    }

    /**
     * Publication operations currently accept only default portals. Console creation of other portals is tracked by PORTAL-380.
     */
    protected boolean isManagedByConsole(Portal portal) {
        return portal.isDefault();
    }
}

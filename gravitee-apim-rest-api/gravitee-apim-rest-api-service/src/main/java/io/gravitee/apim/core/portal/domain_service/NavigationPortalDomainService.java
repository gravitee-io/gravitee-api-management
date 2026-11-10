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
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import lombok.RequiredArgsConstructor;

/**
 * The portal a console navigation write targets: the environment's default portal, and only it. A portal
 * created by the Automation API is never default and its navigation belongs to the Automation API.
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

    /** The reference a navigation item stores to belong to the portal {@link #resolve} returns. */
    public NavigationItemReference.PortalReference resolveReference(String organizationId, String environmentId) {
        return new NavigationItemReference.PortalReference(resolve(organizationId, environmentId).getId());
    }

    /**
     * Like {@link #resolveReference}, but never creates the default portal: the auto-fetch scheduler calls it on a
     * thread whose cached parameters are keyed by name only, so a portal URL read there may be another
     * environment's.
     *
     * @throws NavigationPortalNotResolvedException when the environment has no default portal yet, when its only
     *     portal comes from the Automation API, or when it has several portals
     */
    public NavigationItemReference.PortalReference findReference(String environmentId) {
        try {
            defaultPortalDomainService.checkSinglePortal(environmentId);
        } catch (SeveralPortalsInEnvironmentException e) {
            throw NavigationPortalNotResolvedException.severalPortals(environmentId, e);
        }
        return portalCrudService
            .findDefaultByEnvironmentId(environmentId)
            .map(portal -> new NavigationItemReference.PortalReference(portal.getId()))
            .orElseThrow(() ->
                portalCrudService.findByEnvironmentId(environmentId).isEmpty()
                    ? NavigationPortalNotResolvedException.defaultPortalPostponed(environmentId)
                    : NavigationPortalNotResolvedException.managedByAutomation(environmentId)
            );
    }
}

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
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The portal a console navigation write targets: the environment's default portal, unless the write names
 * another portal of the environment. A portal created by the Automation API is never default.
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
     * {@link #resolve(String, String)}, except that a named portal is used as it is, and that the default portal
     * is found even when the environment has several portals.
     *
     * @throws PortalNotFoundException when the named portal is not a portal of the environment
     */
    public NavigationItemReference.PortalReference resolveReference(
        String organizationId,
        String environmentId,
        @Nullable PortalId portalId
    ) {
        var portal = portalId != null
            ? require(environmentId, portalId)
            : defaultPortalDomainService.findDefault(environmentId).orElseGet(() -> resolve(organizationId, environmentId));
        return new NavigationItemReference.PortalReference(portal.getId());
    }

    /**
     * The portal a read, or a write that only hides content, targets: never created by the call.
     *
     * @return the named portal, else the environment's default portal, else its only portal, which the
     *     Automation API created; empty when there is none of them
     * @throws PortalNotFoundException when the named portal is not a portal of the environment
     */
    public Optional<NavigationItemReference.PortalReference> findReference(String environmentId, @Nullable PortalId portalId) {
        var portal = portalId != null
            ? Optional.of(require(environmentId, portalId))
            : defaultPortalDomainService.findDefaultOrOnly(environmentId);
        return portal.map(found -> new NavigationItemReference.PortalReference(found.getId()));
    }

    private Portal require(String environmentId, PortalId portalId) {
        return portalCrudService
            .findByIdAndEnvironmentId(portalId, environmentId)
            .orElseThrow(() -> new PortalNotFoundException(portalId.toString()));
    }
}

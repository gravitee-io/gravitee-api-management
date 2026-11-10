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
import java.net.URL;
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
    private final PortalUrlDomainService portalUrlDomainService;
    private final PortalContentOwnershipDomainService portalContentOwnershipDomainService;

    /**
     * Creates the environment's default portal when it has no portal at all. A portal the environment
     * already has is never marked default: one created by the Automation API stays as it is. The portal is
     * created with its real URL only: while that URL cannot be resolved (a Cloud environment whose access
     * points are not stored yet, an invalid portal URL setting), creation is postponed to a later call.
     * A portal created here receives the environment's content that names no existing portal.
     *
     * @return the default portal, or empty when the environment's only portal is not the default one or
     *     its creation is postponed
     * @throws SeveralPortalsInEnvironmentException when the environment has several portals
     */
    public Optional<Portal> ensureDefault(String organizationId, String environmentId) {
        var portals = findAtMostOnePortal(environmentId);
        if (portals.isEmpty()) {
            var url = portalUrlDomainService.resolveDefaultPortalUrl(environmentId);
            if (url.isEmpty()) {
                log.warn(
                    "Default portal of environment [{}] not created yet: its portal URL is not set or is not an absolute http(s) URL without credentials",
                    environmentId
                );
                return Optional.empty();
            }
            var created = portalCrudService.create(Portal.createDefault(environmentId, organizationId, DEFAULT_PORTAL_NAME, url.get()));
            log.info("Default portal [{}] created for environment [{}] with URL [{}]", created.getId(), environmentId, created.getUrl());
            var moved = portalContentOwnershipDomainService.moveOrphansTo(created);
            if (!moved.isEmpty()) {
                log.info(
                    "Moved {} navigation item(s) and {} listing(s) of environment [{}] onto its new default portal [{}]",
                    moved.navigationItems(),
                    moved.listings(),
                    environmentId,
                    created.getId()
                );
            }
            return Optional.of(created);
        }
        var portal = portals.getFirst();
        if (portal.isDefault()) {
            return Optional.of(portal);
        }
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

    public Portal getDefault(String environmentId) {
        return findDefault(environmentId).orElseThrow(() -> new DefaultPortalNotFoundException(environmentId));
    }

    /** @throws SeveralPortalsInEnvironmentException when the environment has several portals */
    public void checkSinglePortal(String environmentId) {
        findAtMostOnePortal(environmentId);
    }

    /** Stores the resolved URL on the environment's default portal when it differs from the stored one. */
    public void syncUrl(String environmentId) {
        portalCrudService
            .findDefaultByEnvironmentId(environmentId)
            .ifPresent(portal ->
                resolveUrl(environmentId)
                    .filter(url -> !sameUrl(url, portal.getUrl()))
                    .ifPresent(url -> {
                        portal.changeUrl(url);
                        portalCrudService.update(portal);
                    })
            );
    }

    /**
     * Replaces {@code staleUrl} on every portal of the environment, the default one or not, with the resolved URL.
     *
     * @return the number of portals changed
     */
    public int replaceUrl(String environmentId, URL staleUrl) {
        var portalsWithStaleUrl = portalCrudService
            .findByEnvironmentId(environmentId)
            .stream()
            .filter(portal -> sameUrl(portal.getUrl(), staleUrl))
            .toList();
        if (portalsWithStaleUrl.isEmpty()) {
            return 0;
        }
        return resolveUrl(environmentId)
            .map(url -> {
                portalsWithStaleUrl.forEach(portal -> {
                    portal.changeUrl(url);
                    portalCrudService.update(portal);
                });
                return portalsWithStaleUrl.size();
            })
            .orElse(0);
    }

    private List<Portal> findAtMostOnePortal(String environmentId) {
        var portals = portalCrudService.findByEnvironmentId(environmentId);
        if (portals.size() > 1) {
            throw new SeveralPortalsInEnvironmentException(
                environmentId,
                portals
                    .stream()
                    .map(portal -> "[%s] \"%s\"".formatted(portal.getId(), portal.getName()))
                    .toList()
            );
        }
        return portals;
    }

    private Optional<URL> resolveUrl(String environmentId) {
        var url = portalUrlDomainService.resolveDefaultPortalUrl(environmentId);
        if (url.isEmpty()) {
            log.warn(
                "Portal URL of environment [{}] is not set or is not an absolute http(s) URL: the URL stored on its portal is left as it is until the portal URL can be resolved",
                environmentId
            );
        }
        return url;
    }

    // URL.equals resolves host names; comparing the external forms does not touch the network.
    private static boolean sameUrl(URL a, URL b) {
        return a.toExternalForm().equals(b.toExternalForm());
    }
}

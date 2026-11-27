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
import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalHost;
import io.gravitee.apim.core.portal.model.PortalId;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/** Which access point each portal is served at, across all environments. */
@DomainService
@RequiredArgsConstructor
public class PortalAddressDomainService {

    private static final String NEXT_PORTAL_PATH = "/next";

    private final PortalCrudService portalCrudService;
    private final AccessPointQueryService accessPointQueryService;
    private final AccessPointCrudService accessPointCrudService;
    private final PortalUrlDomainService portalUrlDomainService;
    private final InstallationTypeDomainService installationTypeDomainService;

    /** False on a multi-tenant installation: Cloud owns its access points there, so APIM creates none. */
    public boolean assignsAccessPoints() {
        return !installationTypeDomainService.isMultiTenant();
    }

    /**
     * @return the access point the environment's portals share at its portal URL; empty when that URL is not usable, or when
     * the installation does not {@linkplain #assignsAccessPoints() assign access points}
     */
    public Optional<String> sharedAccessPoint(String environmentId) {
        if (!assignsAccessPoints()) {
            return Optional.empty();
        }
        return portalUrlDomainService.resolveDefaultPortalUrl(environmentId).map(url -> sharedAccessPoint(environmentId, url));
    }

    /** @return the environment's portal URL, ending with the portal app's path; empty when that URL is not usable */
    public Optional<URL> portalUrl(String environmentId) {
        return portalUrlDomainService.resolveDefaultPortalUrl(environmentId);
    }

    /**
     * Moves the portals at the shared access point of {@code previousUrl} to the shared access point of the environment's
     * current portal URL, and releases the previous one, so they follow a change of the portal URL setting. A portal at an
     * access point of its own stays where it is.
     *
     * @return the number of portals moved
     */
    public int followPortalUrl(String environmentId, Optional<URL> previousUrl) {
        if (!assignsAccessPoints() || previousUrl.isEmpty()) {
            return 0;
        }
        var currentUrl = portalUrl(environmentId);
        if (currentUrl.isEmpty()) {
            return 0;
        }
        var previous = findSharedAccessPoint(environmentId, previousUrl.get());
        if (previous.isEmpty() || isAt(previous.get(), currentUrl.get())) {
            return 0;
        }
        var current = sharedAccessPoint(environmentId, currentUrl.get());
        var moved = portalCrudService.findByAccessPointId(previous.get().getId());
        moved.forEach(portal -> {
            portal.linkAccessPoint(current);
            portalCrudService.update(portal);
        });
        if (portalCrudService.findByAccessPointId(previous.get().getId()).isEmpty()) {
            accessPointCrudService.delete(previous.get().getId());
        }
        return moved.size();
    }

    /** The ids of the existing access points the environment's portals can be served at. */
    public Set<String> accessPointIds(String environmentId) {
        return accessPointQueryService
            .getPortalInstanceAccessPoints(environmentId)
            .stream()
            .map(AccessPoint::getId)
            .collect(Collectors.toSet());
    }

    /**
     * The URL of each portal of {@code environmentId} served at an existing access point. A portal at the shared access point
     * is served under the path of the environment's portal URL; one at an access point of its own, at the root of its host.
     */
    public Map<PortalId, URL> resolveUrls(String environmentId, Collection<Portal> portals) {
        var accessPoints = accessPointQueryService
            .getPortalInstanceAccessPoints(environmentId)
            .stream()
            .collect(Collectors.toMap(AccessPoint::getId, Function.identity(), (first, ignored) -> first));
        var portalUrl = portalUrl(environmentId);
        return portals
            .stream()
            .filter(portal -> portal.getAccessPointId() != null && accessPoints.containsKey(portal.getAccessPointId()))
            .collect(Collectors.toMap(Portal::getId, portal -> toUrl(accessPoints.get(portal.getAccessPointId()), portalUrl)));
    }

    /** Deletes the access point of a deleted portal, unless another portal is still served at it. */
    public void release(Portal deletedPortal) {
        var accessPointId = deletedPortal.getAccessPointId();
        if (accessPointId != null && portalCrudService.findByAccessPointId(accessPointId).isEmpty()) {
            accessPointCrudService.delete(accessPointId);
        }
    }

    private String sharedAccessPoint(String environmentId, URL environmentUrl) {
        return findSharedAccessPoint(environmentId, environmentUrl)
            .map(AccessPoint::getId)
            .orElseGet(() -> createAccessPoint(environmentId, environmentUrl.getProtocol(), PortalHost.of(environmentUrl).value()).getId());
    }

    private Optional<AccessPoint> findSharedAccessPoint(String environmentId, URL environmentUrl) {
        return accessPointQueryService
            .getPortalInstanceAccessPoints(environmentId)
            .stream()
            .filter(accessPoint -> isAt(accessPoint, environmentUrl))
            .findFirst();
    }

    private static boolean isAt(AccessPoint accessPoint, URL url) {
        return PortalHost.of(url).value().equals(accessPoint.getHost()) && accessPoint.isSecured() == isSecured(url.getProtocol());
    }

    private AccessPoint createAccessPoint(String environmentId, String scheme, String host) {
        return accessPointCrudService.create(
            AccessPoint.builder()
                .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
                .referenceId(environmentId)
                .target(AccessPoint.Target.PORTAL_INSTANCE)
                .host(host)
                .secured(isSecured(scheme))
                .build()
        );
    }

    private static boolean isSecured(String scheme) {
        return "https".equalsIgnoreCase(scheme);
    }

    private static URL toUrl(AccessPoint accessPoint, Optional<URL> portalUrl) {
        var path = portalUrl
            .filter(url -> isAt(accessPoint, url))
            .map(URL::getPath)
            .orElse(NEXT_PORTAL_PATH);
        try {
            return new URI(accessPoint.buildInstallationAccess() + path).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Access point [%s] does not form a valid portal URL".formatted(accessPoint.getId()), e);
        }
    }
}

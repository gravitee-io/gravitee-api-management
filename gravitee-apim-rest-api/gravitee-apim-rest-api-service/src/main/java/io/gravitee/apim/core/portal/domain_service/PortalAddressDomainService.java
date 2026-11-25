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

    /** @return the access point the environment's portals share at its portal URL; empty when that URL is not usable */
    public Optional<String> sharedAccessPoint(String environmentId) {
        return portalUrlDomainService.resolveDefaultPortalUrl(environmentId).map(url -> sharedAccessPoint(environmentId, url));
    }

    /**
     * The URL of each portal of {@code environmentId} served at an existing access point. Portals are served at the root of
     * their host: the path of the environment's portal URL is not part of it.
     */
    public Map<PortalId, URL> resolveUrls(String environmentId, Collection<Portal> portals) {
        var accessPoints = accessPointQueryService
            .getPortalInstanceAccessPoints(environmentId)
            .stream()
            .collect(Collectors.toMap(AccessPoint::getId, Function.identity(), (first, ignored) -> first));
        return portals
            .stream()
            .filter(portal -> portal.getAccessPointId() != null && accessPoints.containsKey(portal.getAccessPointId()))
            .collect(Collectors.toMap(Portal::getId, portal -> toUrl(accessPoints.get(portal.getAccessPointId()))));
    }

    /** Deletes the access point of a deleted portal, unless another portal is still served at it. */
    public void release(Portal deletedPortal) {
        var accessPointId = deletedPortal.getAccessPointId();
        if (accessPointId != null && portalCrudService.findByAccessPointId(accessPointId).isEmpty()) {
            accessPointCrudService.delete(accessPointId);
        }
    }

    private String sharedAccessPoint(String environmentId, URL environmentUrl) {
        var host = PortalHost.of(environmentUrl).value();
        var secured = isSecured(environmentUrl.getProtocol());
        return accessPointQueryService
            .getPortalInstanceAccessPoints(environmentId)
            .stream()
            .filter(accessPoint -> host.equals(accessPoint.getHost()) && accessPoint.isSecured() == secured)
            .map(AccessPoint::getId)
            .findFirst()
            .orElseGet(() -> createAccessPoint(environmentId, environmentUrl.getProtocol(), host).getId());
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

    private static URL toUrl(AccessPoint accessPoint) {
        try {
            return new URI(accessPoint.buildInstallationAccess() + NEXT_PORTAL_PATH).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Access point [%s] does not form a valid portal URL".formatted(accessPoint.getId()), e);
        }
    }
}

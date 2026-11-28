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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalHost;
import io.gravitee.apim.core.portal.model.PortalHostTemplate;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.utils.StringUtils;
import jakarta.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
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

    private static final String URL_EXISTS_TECHNICAL_CODE = "portal.url.exists";
    private static final String FALLBACK_SLUG = "portal";
    private static final String NEXT_PORTAL_PATH = "/next";
    // Below PortalHostTemplate.MAX_SLUG_LENGTH, so a collision suffix always fits.
    private static final int MAX_BASE_SLUG_LENGTH = 40;
    private static final Set<String> HTTP_SCHEMES = Set.of("http", "https");

    private final PortalCrudService portalCrudService;
    private final AccessPointQueryService accessPointQueryService;
    private final AccessPointCrudService accessPointCrudService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final PortalUrlDomainService portalUrlDomainService;
    private final InstallationTypeDomainService installationTypeDomainService;

    /**
     * The access point of a new portal: one on a generated host, or without a configured host template, the one the
     * environment's portals share at its portal URL.
     *
     * @return empty when the environment has no usable portal URL, or when the installation does not
     * {@linkplain #assignsAccessPoints() assign access points}
     */
    public Optional<String> newPortalAccessPoint(String environmentId, String portalName) {
        if (!assignsAccessPoints()) {
            return Optional.empty();
        }
        var environmentUrl = portalUrlDomainService.resolveDefaultPortalUrl(environmentId);
        if (environmentUrl.isEmpty()) {
            return Optional.empty();
        }
        var template = installationAccessQueryService.getPortalHostTemplate();
        if (template.isEmpty()) {
            return Optional.of(sharedAccessPoint(environmentId, environmentUrl.get()));
        }
        var url = environmentUrl.get();
        var host = firstFreeHost(url, template.get(), portalName);
        return Optional.of(createAccessPoint(environmentId, url.getProtocol(), host).getId());
    }

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

    /**
     * @param portalId the portal the host may already be given to; null for a portal not stored yet
     * @return {@code rawUrl} with a lowercase host: an access point keeps a scheme, a host and a port, not a path
     * @throws ValidationDomainException when it cannot serve a portal, or its host belongs to another portal
     */
    public URL validateCustomUrl(@Nullable PortalId portalId, String rawUrl) {
        var url = parseCustomUrl(rawUrl);
        var host = PortalHost.of(url);
        // Like a generated host, a custom one must not shadow a console, an API or another portal, nor get a second access point.
        var taken = accessPointQueryService
            .findByHost(host.value())
            .stream()
            .anyMatch(accessPoint -> accessPoint.getTarget() != AccessPoint.Target.PORTAL_INSTANCE || !servesOnly(accessPoint, portalId));
        if (taken) {
            throw new ValidationDomainException("Host [%s] is already in use".formatted(host.value()), URL_EXISTS_TECHNICAL_CODE);
        }
        return url;
    }

    /** @return the access point of a portal served at a custom URL, {@linkplain #validateCustomUrl validated} first */
    public String newCustomAccessPoint(String environmentId, URL customUrl) {
        return createAccessPoint(environmentId, customUrl.getProtocol(), PortalHost.of(customUrl).value()).getId();
    }

    /**
     * @param scopeEnvironmentId limits the lookup to this environment; null searches all of them
     * @return the one portal on this host; empty when there is none, or when portals stored before each portal had its own
     *     address share it
     */
    public Optional<Portal> findByAddress(PortalHost host, @Nullable String scopeEnvironmentId) {
        var candidates = accessPointQueryService
            .findPortalInstancesByHost(host.value())
            .stream()
            .filter(accessPoint -> scopeEnvironmentId == null || scopeEnvironmentId.equals(accessPoint.getReferenceId()))
            .flatMap(accessPoint -> portalCrudService.findByAccessPointId(accessPoint.getId()).stream())
            .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    /**
     * @return the scheme, host and port of the portal's own access point; empty when it has none, when it is served at the
     *     environment's portal URL, or when that URL cannot be resolved to tell
     */
    public Optional<String> ownOrigin(Portal portal) {
        var environmentUrl = portalUrl(portal.getEnvironmentId());
        if (portal.getAccessPointId() == null || environmentUrl.isEmpty()) {
            return Optional.empty();
        }
        return accessPointQueryService
            .getPortalInstanceAccessPoints(portal.getEnvironmentId())
            .stream()
            .filter(accessPoint -> accessPoint.getId().equals(portal.getAccessPointId()))
            .filter(accessPoint -> !isAt(accessPoint, environmentUrl.get()))
            .map(AccessPoint::buildInstallationAccess)
            .findFirst();
    }

    /** Deletes the access point of a deleted or never stored portal, unless another portal is served at it. */
    public void release(Portal portal) {
        var accessPointId = portal.getAccessPointId();
        if (accessPointId != null && portalCrudService.findByAccessPointId(accessPointId).isEmpty()) {
            accessPointCrudService.delete(accessPointId);
        }
    }

    private boolean servesOnly(AccessPoint accessPoint, @Nullable PortalId portalId) {
        var served = portalCrudService.findByAccessPointId(accessPoint.getId());
        return !served.isEmpty() && served.stream().allMatch(portal -> portal.getId().equals(portalId));
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

    private String firstFreeHost(URL environmentUrl, PortalHostTemplate template, String portalName) {
        var slug = baseSlug(portalName);
        var host = hostAndPort(environmentUrl, template.hostFor(slug));
        for (int suffix = 2; isTaken(host); suffix++) {
            host = hostAndPort(environmentUrl, template.hostFor(slug + "-" + suffix));
        }
        return host;
    }

    // Any access point, whatever its target: a generated portal host must not shadow a console, an API or another portal.
    private boolean isTaken(String host) {
        return !accessPointQueryService.findByHost(host).isEmpty();
    }

    private static String hostAndPort(URL environmentUrl, String host) {
        var port = environmentUrl.getPort();
        return port == -1 || port == environmentUrl.getDefaultPort() ? host : host + ":" + port;
    }

    private static String baseSlug(String portalName) {
        var slug = StringUtils.slugify(portalName);
        if (slug == null || slug.isEmpty()) {
            return FALLBACK_SLUG;
        }
        var shortened = slug.substring(0, Math.min(slug.length(), MAX_BASE_SLUG_LENGTH));
        return shortened.endsWith("-") ? shortened.substring(0, shortened.length() - 1) : shortened;
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

    private static URL parseCustomUrl(String rawUrl) {
        try {
            var uri = new URI(rawUrl);
            // A user-info part would put credentials in the stored host and in the logs.
            if (
                uri.getScheme() == null ||
                !HTTP_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT)) ||
                uri.getHost() == null ||
                uri.getRawUserInfo() != null ||
                uri.getRawQuery() != null ||
                uri.getRawFragment() != null ||
                (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath()))
            ) {
                throw invalidCustomUrl(null);
            }
            return new URI(
                uri.getScheme().toLowerCase(Locale.ROOT),
                null,
                uri.getHost().toLowerCase(Locale.ROOT),
                uri.getPort(),
                null,
                null,
                null
            ).toURL();
        } catch (URISyntaxException | MalformedURLException | NullPointerException e) {
            throw invalidCustomUrl(e);
        }
    }

    private static ValidationDomainException invalidCustomUrl(@Nullable Exception cause) {
        var message = "A portal URL must be an absolute http(s) URL with no path, credentials, query string or fragment";
        return cause == null ? new ValidationDomainException(message) : new ValidationDomainException(message, cause);
    }
}

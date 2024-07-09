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
package io.gravitee.apim.infra.query_service.installation;

import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class InstallationAccessQueryServiceImpl implements InstallationAccessQueryService {

    private static final String INSTALLATION_STANDALONE_PROPERTY = "installation." + InstallationType.Labels.STANDALONE + ".";
    private static final String DEFAULT_ID = GraviteeContext.getDefaultOrganization();
    private final ConfigurableEnvironment environment;
    private final InstallationTypeDomainService installationTypeDomainService;
    private final AccessPointQueryService accessPointQueryService;

    @Value("${cockpit.enabled:false}")
    private boolean cockpitEnabled;

    @Value("${installation.api.console.url:${installation.api.url:#{null}}}")
    private String consoleApiUrl;

    @Value("${installation.api.portal.url:${installation.api.url:#{null}}}")
    private String portalApiUrl;

    @Value("${installation.api.proxyPath.management:${http.api.management.entrypoint:${http.api.entrypoint:/}management}}")
    private String managementProxyPath;

    @Value("${installation.api.proxyPath.portal:${http.api.portal.entrypoint:${http.api.entrypoint:/}portal}}")
    private String portalProxyPath;

    private final Map<String, String> consoleUrls = new HashMap<>();
    private final Map<String, String> portalUrls = new HashMap<>();

    @PostConstruct
    public void afterPropertiesSet() {
        if (!installationTypeDomainService.isMultiTenant()) {
            consoleUrls.putAll(loadUrls("console", "orgId"));
            portalUrls.putAll(loadUrls("portal", "envId"));

            // Handle legacy urls
            handleLegacyUrls();

            // Setup default value if required
            if (cockpitEnabled) {
                if (consoleUrls.isEmpty()) {
                    consoleUrls.put(DEFAULT_ID, "http://localhost:4000");
                }
                if (portalUrls.isEmpty()) {
                    portalUrls.put(DEFAULT_ID, "http://localhost:4100");
                }
            }

            // Validate api url
            validateApiUrl(consoleApiUrl);
            validateApiUrl(portalApiUrl);
        }
    }

    private void validateApiUrl(String apiURL) {
        if (apiURL != null) {
            try {
                URL url = URI.create(apiURL).toURL();
                if (!isValidDomainName(url.getHost())) {
                    throw new InvalidInstallationUrlException("API url '%s' is malformed.".formatted(apiURL));
                }
            } catch (Exception e) {
                throw new InvalidInstallationUrlException("API url '%s' must be a valid URL.".formatted(apiURL));
            }
        }
    }

    private void handleLegacyUrls() {
        if (consoleUrls.isEmpty()) {
            String legacyUIUrl = environment.getProperty("console.ui.url");
            if (legacyUIUrl != null) {
                consoleUrls.put(DEFAULT_ID, legacyUIUrl);
            }
        }

        if (portalUrls.isEmpty()) {
            String legacyPortalUrl = environment.getProperty("console.portal.url");
            if (legacyPortalUrl != null) {
                portalUrls.put(DEFAULT_ID, legacyPortalUrl);
            }
        }

        if (consoleApiUrl == null) {
            try {
                String legacyApiUrl = environment.getProperty("console.api.url");
                if (legacyApiUrl != null) {
                    URI legacyApiURI = URI.create(legacyApiUrl);
                    this.managementProxyPath = legacyApiURI.getPath();
                    String url = legacyApiURI.resolve("/").toString();
                    this.consoleApiUrl = url;
                    this.portalApiUrl = url;
                }
            } catch (Exception e) {
                log.warn("Unable to parse legacy url configuration [console.api.url]", e);
            }
        }
    }

    private Map<String, String> loadUrls(final String keyUI, final String keyId) {
        Map<String, String> urls = new HashMap<>();
        int idx = 0;
        boolean hasMany = environment.containsProperty(INSTALLATION_STANDALONE_PROPERTY + keyUI + ".urls[" + idx + "]." + keyId);
        if (hasMany) {
            boolean hasNext = true;
            while (hasNext) {
                String id = environment.getProperty(INSTALLATION_STANDALONE_PROPERTY + keyUI + ".urls[" + idx + "]." + keyId);
                hasNext = (id != null);
                if (hasNext) {
                    String url = environment.getProperty(INSTALLATION_STANDALONE_PROPERTY + keyUI + ".urls[" + idx + "].url");
                    validateUrl(keyUI, url);
                    urls.put(id, url);
                }
                idx++;
            }
        } else {
            String uiUrl = environment.getProperty(INSTALLATION_STANDALONE_PROPERTY + keyUI + ".url");
            if (uiUrl != null) {
                validateUrl(keyUI, uiUrl);
                urls.put(DEFAULT_ID, uiUrl);
            }
        }
        return urls;
    }

    private void validateUrl(final String keyUI, final String urlAsString) {
        if (urlAsString == null) {
            throw new InvalidInstallationUrlException("Installation '%s' url  cannot be null or empty.".formatted(keyUI));
        }
        URL url;
        try {
            url = URI.create(urlAsString).toURL();
        } catch (Exception e) {
            throw new InvalidInstallationUrlException("Installation '%s' url '%s' must be a valid URL.".formatted(keyUI, urlAsString));
        }
        validateHost(keyUI, url.getHost());
    }

    private void validateHost(final String keyUI, final String hostAsString) {
        if (!isValidDomainName(hostAsString)) {
            throw new InvalidInstallationUrlException("Installation '%s' url '%s' is malformed.".formatted(keyUI, hostAsString));
        }
    }

    public static boolean isValidDomainName(String domain) {
        if (Strings.isNullOrEmpty(domain)) {
            return false;
        }
        return InternetDomainName.isValid(domain);
    }

    @Override
    public String getConsoleApiPath() {
        return managementProxyPath;
    }

    @Override
    public String getPortalApiPath() {
        return portalProxyPath;
    }

    @Override
    public List<String> getConsoleUrls() {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getConsoleAccessPoints();
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            return new ArrayList<>(consoleUrls.values());
        }
    }

    @Override
    public List<String> getConsoleUrls(final String organizationId) {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getConsoleAccessPoints(organizationId);
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            String consoleUrl = consoleUrls.get(organizationId);
            return consoleUrl == null ? List.of() : List.of(consoleUrl);
        }
    }

    @Override
    public String getConsoleUrl(final String organizationId) {
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint accessPoint = accessPointQueryService.getConsoleAccessPoint(organizationId);
            return buildHttpUrl(accessPoint);
        } else {
            return consoleUrls.get(organizationId);
        }
    }

    @Override
    public String getConsoleAPIUrl(final String organizationId) {
        String consoleAPIBaseUrl;
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getConsoleApiAccessPoint(organizationId);
            consoleAPIBaseUrl = buildHttpUrl(consoleAccessPoint);
        } else {
            consoleAPIBaseUrl = consoleApiUrl;
        }
        if (consoleAPIBaseUrl != null) {
            URI fullUrl = URI.create(consoleAPIBaseUrl).resolve(managementProxyPath);
            return fullUrl.toString();
        }
        return null;
    }

    @Override
    public List<String> getPortalUrls() {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getPortalAccessPoints();
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            return new ArrayList<>(portalUrls.values());
        }
    }

    @Override
    public List<String> getPortalUrls(final String environmentId) {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getPortalAccessPoints(environmentId);
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            String portalUrl = portalUrls.get(environmentId);
            return portalUrl == null ? List.of() : List.of(portalUrl);
        }
    }

    @Override
    public String getPortalUrl(final String environmentId) {
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getPortalAccessPoint(environmentId);
            return buildHttpUrl(consoleAccessPoint);
        } else {
            return portalUrls.get(environmentId);
        }
    }

    @Override
    public String getPortalAPIUrl(final String environmentId) {
        String portalAPIBaseUrl;
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getPortalApiAccessPoint(environmentId);
            portalAPIBaseUrl = buildHttpUrl(consoleAccessPoint);
        } else {
            portalAPIBaseUrl = portalApiUrl;
        }
        if (portalAPIBaseUrl != null) {
            URI fullUrl = URI.create(portalAPIBaseUrl).resolve(portalProxyPath);
            return fullUrl.toString();
        }
        return null;
    }

    @Override
    public List<RestrictedDomain> getGatewayRestrictedDomains(final String environmentId) {
        if (installationTypeDomainService.isMultiTenant()) {
            return accessPointQueryService
                .getGatewayAccessPoints(environmentId)
                .stream()
                .map(accessPoint -> RestrictedDomain.builder().domain(accessPoint.getHost()).secured(accessPoint.isSecured()).build())
                .toList();
        } else {
            return List.of();
        }
    }

    private String buildHttpUrl(final AccessPoint accessPoint) {
        if (accessPoint != null) {
            return accessPoint.buildInstallationAccess();
        }
        return null;
    }

    static class InvalidInstallationUrlException extends RuntimeException {

        public InvalidInstallationUrlException(final String message) {
            super(message);
        }
    }
}

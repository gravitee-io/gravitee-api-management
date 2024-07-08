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
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class InstallationAccessQueryServiceImpl implements InstallationAccessQueryService {

    private static final String INSTALLATION_STANDALONE_PROPERTY = "installation." + InstallationType.Labels.STANDALONE + ".";
    private static final String DEFAULT_ID = GraviteeContext.getDefaultOrganization();
    private final ConfigurableEnvironment environment;
    private final InstallationTypeDomainService installationTypeDomainService;
    private final AccessPointQueryService accessPointQueryService;
    private final ParameterService parameterService;
    private final OrganizationService organizationService;
    private final EnvironmentService environmentService;

    @Value("${installation.api.url:#{null}}")
    private String apiURL;

    @Value("${installation.api.console.url:#{null}}")
    private String consoleApiUrl;

    @Value("${installation.api.portal.url:#{null}}")
    private String portalApiUrl;

    @Value("${installation.api.proxyPath.management:${http.api.management.entrypoint:${http.api.entrypoint:/}management}}")
    private String managementProxyPath;

    @Value("${installation.api.proxyPath.portal:${http.api.portal.entrypoint:${http.api.entrypoint:/}portal}}")
    private String portalProxyPath;

    private final Map<String, String> consoleUrls = new HashMap<>();
    private final Map<String, String> portalUrls = new HashMap<>();

    public InstallationAccessQueryServiceImpl(
        final ConfigurableEnvironment environment,
        final InstallationTypeDomainService installationTypeDomainService,
        final AccessPointQueryService accessPointQueryService,
        final ParameterService parameterService,
        @Lazy final OrganizationService organizationService,
        @Lazy final EnvironmentService environmentService
    ) {
        this.environment = environment;
        this.installationTypeDomainService = installationTypeDomainService;
        this.accessPointQueryService = accessPointQueryService;
        this.parameterService = parameterService;
        this.organizationService = organizationService;
        this.environmentService = environmentService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        if (!installationTypeDomainService.isMultiTenant()) {
            consoleUrls.putAll(loadUrls("console", "orgId"));
            portalUrls.putAll(loadUrls("portal", "envId"));

            // Handle legacy urls
            handleEnvironmentUrls();

            // Validate api url
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
    }

    private void handleEnvironmentUrls() {
        if (consoleUrls.isEmpty()) {
            String legacyPortalUrl = environment.getProperty("management.url");
            if (legacyPortalUrl != null) {
                consoleUrls.put(DEFAULT_ID, legacyPortalUrl);
            } else {
                String legacyUIUrl = environment.getProperty("console.ui.url");
                if (legacyUIUrl != null) {
                    consoleUrls.put(DEFAULT_ID, legacyUIUrl);
                }
            }
        }

        if (portalUrls.isEmpty()) {
            String legacyPortalUrl = environment.getProperty("portal.url");
            if (legacyPortalUrl != null) {
                portalUrls.put(DEFAULT_ID, legacyPortalUrl);
            } else {
                legacyPortalUrl = environment.getProperty("console.portal.url");
                if (legacyPortalUrl != null) {
                    portalUrls.put(DEFAULT_ID, legacyPortalUrl);
                }
            }
        }

        if (apiURL == null) {
            try {
                String legacyApiUrl = environment.getProperty("console.api.url");
                if (legacyApiUrl != null) {
                    URI legacyApiURI = URI.create(legacyApiUrl);
                    this.managementProxyPath = legacyApiURI.getPath();
                    this.apiURL = legacyApiURI.resolve("/").toString();
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
            Collection<OrganizationEntity> organizations = organizationService.findAll();
            if (organizations == null || organizations.isEmpty()) {
                OrganizationEntity organization = organizationService.getDefaultOrInitialize();
                return List.of(getConsoleUrlFromEnv(organization.getId()));
            } else {
                return organizations.stream().map(OrganizationEntity::getId).map(this::getConsoleUrlFromEnv).toList();
            }
        }
    }

    @Override
    public List<String> getConsoleUrls(final String organizationId) {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getConsoleAccessPoints(organizationId);
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            String consoleUrl = getConsoleUrlFromEnv(organizationId);
            return List.of(consoleUrl);
        }
    }

    @Override
    public String getConsoleUrl(final String organizationId) {
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint accessPoint = accessPointQueryService.getConsoleAccessPoint(organizationId);
            return buildHttpUrl(accessPoint);
        } else {
            return getConsoleUrlFromEnv(organizationId);
        }
    }

    @NonNull
    private String getConsoleUrlFromEnv(final String organizationId) {
        String consoleUrl = consoleUrls.get(organizationId);
        if (consoleUrl == null || consoleUrl.equals(DEFAULT_CONSOLE_URL)) {
            consoleUrl =
                parameterService.find(
                    GraviteeContext.getExecutionContext(),
                    Key.MANAGEMENT_URL,
                    organizationId,
                    ParameterReferenceType.ORGANIZATION
                );
            if (consoleUrl == null) {
                consoleUrl = Key.MANAGEMENT_URL.defaultValue();
            }
        }
        if (consoleUrl == null) {
            consoleUrl = DEFAULT_CONSOLE_URL;
        }
        return consoleUrl;
    }

    @Override
    public String getConsoleAPIUrl(final String organizationId) {
        String consoleAPIBaseUrl;
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getConsoleApiAccessPoint(organizationId);
            consoleAPIBaseUrl = buildHttpUrl(consoleAccessPoint);
        } else if (StringUtils.isNotEmpty(consoleApiUrl)) {
            consoleAPIBaseUrl = consoleApiUrl;
        } else {
            consoleAPIBaseUrl = apiURL;
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
            return environmentService.findAllOrInitialize().stream().map(EnvironmentEntity::getId).map(this::getPortalUrlFromEnv).toList();
        }
    }

    @Override
    public List<String> getPortalUrls(final String environmentId) {
        if (installationTypeDomainService.isMultiTenant()) {
            List<AccessPoint> accessPoints = accessPointQueryService.getPortalAccessPoints(environmentId);
            return accessPoints.stream().map(this::buildHttpUrl).toList();
        } else {
            String portalUrl = getPortalUrlFromEnv(environmentId);
            return List.of(portalUrl);
        }
    }

    @Override
    public String getPortalUrl(final String environmentId) {
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getPortalAccessPoint(environmentId);
            return buildHttpUrl(consoleAccessPoint);
        } else {
            return getPortalUrlFromEnv(environmentId);
        }
    }

    @NonNull
    private String getPortalUrlFromEnv(final String environmentId) {
        String portalUrl = portalUrls.get(environmentId);
        if (portalUrl == null || portalUrl.equals(DEFAULT_PORTAL_URL)) {
            portalUrl =
                parameterService.find(
                    GraviteeContext.getExecutionContext(),
                    Key.PORTAL_URL,
                    environmentId,
                    ParameterReferenceType.ENVIRONMENT
                );
            if (portalUrl == null) {
                portalUrl = Key.PORTAL_URL.defaultValue();
            }
        }
        if (portalUrl == null) {
            portalUrl = DEFAULT_PORTAL_URL;
        }
        return portalUrl;
    }

    @Override
    public String getPortalAPIUrl(final String environmentId) {
        String portalAPIBaseUrl;
        if (installationTypeDomainService.isMultiTenant()) {
            AccessPoint consoleAccessPoint = accessPointQueryService.getPortalApiAccessPoint(environmentId);
            portalAPIBaseUrl = buildHttpUrl(consoleAccessPoint);
        } else if (StringUtils.isNotEmpty(portalApiUrl)) {
            portalAPIBaseUrl = portalApiUrl;
        } else {
            portalAPIBaseUrl = apiURL;
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

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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.EntrypointNotFoundException;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiEntrypointServiceImpl implements ApiEntrypointService {

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");
    private static final Pattern PROTOCOL_REMOVER = Pattern.compile("^[a-zA-Z]+://");
    // RFC 6454 section-7.1, serialized-origin regex from RFC 3986
    private static final String URI_PATH_SEPARATOR = "/";
    public static final Set<DefinitionVersion> APIS_WITHOUT_ENTRYPOINT = EnumSet.of(
        DefinitionVersion.FEDERATED,
        DefinitionVersion.FEDERATED_AGENT
    );

    private final ParameterService parameterService;
    private final EntrypointService entrypointService;
    private final AccessPointQueryService accessPointQueryService;

    public ApiEntrypointServiceImpl(
        final ParameterService parameterService,
        final EntrypointService entrypointService,
        final AccessPointQueryService accessPointQueryService
    ) {
        this.parameterService = parameterService;
        this.entrypointService = entrypointService;
        this.accessPointQueryService = accessPointQueryService;
    }

    @Override
    public List<ApiEntrypointEntity> getApiEntrypoints(final ExecutionContext executionContext, final GenericApiEntity genericApiEntity) {
        List<ApiEntrypointEntity> apiEntrypoints = new ArrayList<>();

        if (APIS_WITHOUT_ENTRYPOINT.contains(genericApiEntity.getDefinitionVersion())) {
            return apiEntrypoints;
        }

        String defaultTcpPort = parameterService.find(
            executionContext,
            Key.PORTAL_TCP_PORT,
            executionContext.getEnvironmentId(),
            ParameterReferenceType.ENVIRONMENT
        );
        String defaultKafkaDomain = parameterService.find(
            executionContext,
            Key.PORTAL_KAFKA_DOMAIN,
            executionContext.getEnvironmentId(),
            ParameterReferenceType.ENVIRONMENT
        );
        String defaultKafkaPort = parameterService.find(
            executionContext,
            Key.PORTAL_KAFKA_PORT,
            executionContext.getEnvironmentId(),
            ParameterReferenceType.ENVIRONMENT
        );

        if (genericApiEntity.getTags() != null && !genericApiEntity.getTags().isEmpty()) {
            List<EntrypointEntity> organizationEntrypoints = entrypointService.findAll(executionContext);

            organizationEntrypoints.forEach(entrypoint -> {
                // Check if organizationEntrypoints is matching all tags of the API
                boolean isEntrypointMatching = Arrays.stream(entrypoint.getTags()).allMatch(tag ->
                    genericApiEntity.getTags().stream().toList().contains(tag)
                );
                if (!isEntrypointMatching) {
                    return;
                }
                if (!hasSupportedListeners(genericApiEntity)) {
                    return;
                }
                // Check if entrypoint is matching the API target
                if (entrypoint.getTarget() != getApiTarget(genericApiEntity)) {
                    return;
                }

                final String entrypointValue = entrypoint.getValue();

                apiEntrypoints.addAll(
                    getEntrypoints(
                        genericApiEntity,
                        entrypointValue,
                        defaultTcpPort,
                        entrypointValue.lastIndexOf(":") > 0
                            ? entrypointValue.substring(0, entrypointValue.lastIndexOf(":"))
                            : defaultKafkaDomain,
                        entrypointValue.lastIndexOf(":") > 0
                            ? entrypointValue.substring(entrypointValue.lastIndexOf(":") + 1)
                            : defaultKafkaPort,
                        genericApiEntity.getTags(),
                        executionContext.getEnvironmentId()
                    )
                );
            });
        }

        // If empty, get the default entrypoint
        if (apiEntrypoints.isEmpty()) {
            String defaultEntrypoint = parameterService.find(
                executionContext,
                Key.PORTAL_ENTRYPOINT,
                executionContext.getEnvironmentId(),
                ParameterReferenceType.ENVIRONMENT
            );

            apiEntrypoints.addAll(
                getEntrypoints(
                    genericApiEntity,
                    defaultEntrypoint,
                    defaultTcpPort,
                    defaultKafkaDomain,
                    defaultKafkaPort,
                    null,
                    executionContext.getEnvironmentId()
                )
            );
        }

        return apiEntrypoints;
    }

    private List<ApiEntrypointEntity> getEntrypoints(
        final GenericApiEntity genericApiEntity,
        final String entrypointValue,
        final String tcpPort,
        final String kafkaDomain,
        final String kafkaPort,
        final Set<String> tagEntrypoints,
        final String environmentId
    ) {
        if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity api = (ApiEntity) genericApiEntity;
            return api
                .getProxy()
                .getVirtualHosts()
                .stream()
                .flatMap(virtualHost ->
                    getHttpApiEntrypointEntity(
                        entrypointValue,
                        virtualHost.getHost(),
                        virtualHost.getPath(),
                        virtualHost.isOverrideEntrypoint(),
                        tagEntrypoints,
                        environmentId
                    ).stream()
                )
                .toList();
        } else if (genericApiEntity.getDefinitionVersion() == DefinitionVersion.V4 && genericApiEntity instanceof NativeApiEntity api) {
            return api
                .getListeners()
                .stream()
                .filter(listener -> listener instanceof KafkaListener)
                .flatMap(listener -> {
                    var kafkaListener = (KafkaListener) listener;
                    return getKafkaNativeApiEntrypointEntity(
                        kafkaListener.getHost(),
                        kafkaDomain,
                        kafkaPort,
                        tagEntrypoints,
                        environmentId
                    ).stream();
                })
                .toList();
        } else {
            io.gravitee.rest.api.model.v4.api.ApiEntity api = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
            return api
                .getListeners()
                .stream()
                .flatMap(listener -> {
                    if (listener instanceof HttpListener httpListener) {
                        return httpListener
                            .getPaths()
                            .stream()
                            .flatMap(path ->
                                getHttpApiEntrypointEntity(
                                    entrypointValue,
                                    path.getHost(),
                                    path.getPath(),
                                    path.isOverrideAccess(),
                                    tagEntrypoints,
                                    environmentId
                                ).stream()
                            );
                    } else if (listener instanceof TcpListener tcpListener) {
                        return tcpListener
                            .getHosts()
                            .stream()
                            .map(tcpHost -> getTcpApiEntrypointEntity(tcpHost, tcpPort, entrypointValue, tagEntrypoints));
                    } else return Stream.empty();
                })
                .toList();
        }
    }

    private List<ApiEntrypointEntity> getHttpApiEntrypointEntity(
        final String entrypointHost,
        final String host,
        final String path,
        final boolean isOverride,
        final Set<String> tags,
        final String environmentId
    ) {
        final String defaultScheme = getScheme(entrypointHost);

        List<ApiEntrypointEntity> entrypoints = new ArrayList<>();

        if (host == null || !isOverride) {
            List<AccessPoint> accessPoints = this.accessPointQueryService.getGatewayAccessPoints(environmentId);
            if (accessPoints.isEmpty()) {
                entrypoints.add(createHttpApiEntrypointEntity(defaultScheme, entrypointHost, path, tags, host));
            } else if (tags != null && !tags.isEmpty()) {
                // Extract hostname from entrypoint URL for secure domain matching
                String hostname = extractHostname(entrypointHost);
                boolean matchesEnvironmentAccessPoint = accessPoints.stream().anyMatch(ap -> isSubdomainOrEqual(hostname, ap.getHost()));
                if (matchesEnvironmentAccessPoint) {
                    entrypoints.add(createHttpApiEntrypointEntity(defaultScheme, entrypointHost, path, tags, host));
                }
            } else {
                for (AccessPoint accessPoint : accessPoints) {
                    String targetHost = accessPoint.getHost();
                    String scheme = accessPoint.isSecured() ? "https" : "http";
                    entrypoints.add(createHttpApiEntrypointEntity(scheme, targetHost, path, tags, targetHost));
                }
            }
        } else {
            entrypoints.add(createHttpApiEntrypointEntity(defaultScheme, host, path, tags, host));
        }

        return entrypoints;
    }

    ApiEntrypointEntity createHttpApiEntrypointEntity(
        String defaultScheme,
        String host,
        String path,
        Set<String> tags,
        String originalHost
    ) {
        if (!host.toLowerCase().startsWith("http")) {
            host = defaultScheme + "://" + host;
        }

        String url = DUPLICATE_SLASH_REMOVER.matcher(host + URI_PATH_SEPARATOR + path).replaceAll(URI_PATH_SEPARATOR);
        if (url.endsWith(URI_PATH_SEPARATOR)) {
            url = url.substring(0, url.length() - URI_PATH_SEPARATOR.length());
        }

        return new ApiEntrypointEntity(tags, url, originalHost);
    }

    private ApiEntrypointEntity getTcpApiEntrypointEntity(
        final String tcpHost,
        final String tcpPort,
        final String host,
        final Set<String> tags
    ) {
        String target = String.join(":", tcpHost, tcpPort);
        return new ApiEntrypointEntity(tags, target, host);
    }

    private List<ApiEntrypointEntity> getKafkaNativeApiEntrypointEntity(
        final String host,
        final String domain,
        final String port,
        final Set<String> tags,
        final String environmentId
    ) {
        List<ApiEntrypointEntity> entrypoints = new ArrayList<>();
        List<AccessPoint> accessPoints = this.accessPointQueryService.getKafkaGatewayAccessPoints(environmentId);

        if (accessPoints.isEmpty()) {
            entrypoints.add(createKafkaNativeApiEntrypointEntity(host, domain, port, tags));
        } else if (tags != null && !tags.isEmpty()) {
            boolean matchesEnvironmentAccessPoint = accessPoints
                .stream()
                .anyMatch(ap -> {
                    String accessPointDomain = extractHostname(ap.getHost());
                    // Bidirectional subdomain check for Kafka
                    return isSubdomainOrEqual(domain, accessPointDomain) || isSubdomainOrEqual(accessPointDomain, domain);
                });
            if (matchesEnvironmentAccessPoint) {
                entrypoints.add(createKafkaNativeApiEntrypointEntity(host, domain, port, tags));
            }
        } else {
            for (AccessPoint accessPoint : accessPoints) {
                String accessPointDomain = accessPoint.getHost();
                String targetDomain = accessPointDomain.contains(":")
                    ? accessPointDomain.substring(0, accessPointDomain.indexOf(":"))
                    : accessPointDomain;
                String targetPort = accessPointDomain.contains(":")
                    ? accessPointDomain.substring(accessPointDomain.indexOf(":") + 1)
                    : port;

                entrypoints.add(createKafkaNativeApiEntrypointEntity(host, targetDomain, targetPort, tags));
            }
        }
        return entrypoints;
    }

    private ApiEntrypointEntity createKafkaNativeApiEntrypointEntity(
        final String host,
        final String domain,
        final String port,
        final Set<String> tags
    ) {
        String kafkaDomain;
        if (domain == null || domain.isBlank()) {
            kafkaDomain = host;
        } else if (domain.contains("{apiHost}")) {
            kafkaDomain = domain.replace("{apiHost}", host);
        } else {
            kafkaDomain = host + "." + domain;
        }
        var target = kafkaDomain + ":" + port;
        return new ApiEntrypointEntity(tags, target, host);
    }

    private String getScheme(String entrypointValue) {
        String scheme = "https";
        if (entrypointValue != null) {
            try {
                scheme = new URL(entrypointValue).getProtocol();
            } catch (MalformedURLException e) {
                // return default scheme
            }
        }
        return scheme;
    }

    private String extractHostname(String urlOrHost) {
        if (urlOrHost == null || urlOrHost.isBlank()) {
            return null;
        }

        try {
            String urlWithoutProtocol = PROTOCOL_REMOVER.matcher(urlOrHost).replaceFirst("");
            int portIndex = urlWithoutProtocol.indexOf(':');
            if (portIndex > 0) {
                urlWithoutProtocol = urlWithoutProtocol.substring(0, portIndex);
            }
            return urlWithoutProtocol.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSubdomainOrEqual(String hostname, String accessPointHost) {
        if (hostname == null || accessPointHost == null) {
            return false;
        }

        String normalizedHostname = hostname.toLowerCase();
        String normalizedAccessPoint = accessPointHost.toLowerCase();

        if (normalizedHostname.equals(normalizedAccessPoint)) {
            return true;
        }

        return normalizedHostname.endsWith("." + normalizedAccessPoint);
    }

    public String getApiEntrypointsListenerType(GenericApiEntity genericApiEntity) {
        if (
            genericApiEntity.getDefinitionVersion() == DefinitionVersion.V1 ||
            genericApiEntity.getDefinitionVersion() == DefinitionVersion.V2
        ) {
            return "HTTP";
        }

        if (genericApiEntity instanceof NativeApiEntity api) {
            return api
                .getListeners()
                .stream()
                .findFirst()
                .map(listener -> listener.getType().toString())
                .orElseThrow(() -> new EntrypointNotFoundException(api.getId()));
        }
        io.gravitee.rest.api.model.v4.api.ApiEntity api = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
        return api
            .getListeners()
            .stream()
            .findFirst()
            .map(listener -> listener.getType().toString())
            .orElseThrow(() -> new EntrypointNotFoundException(api.getId()));
    }

    private EntrypointEntity.Target getApiTarget(final GenericApiEntity genericApiEntity) {
        // V1 or V2 API
        if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            return EntrypointEntity.Target.HTTP;
        }
        // V4 Native API
        if (genericApiEntity instanceof NativeApiEntity nativeApiEntity) {
            if (
                nativeApiEntity
                    .getListeners()
                    .stream()
                    .anyMatch(listener -> listener instanceof KafkaListener)
            ) {
                return EntrypointEntity.Target.KAFKA;
            }
        }
        // V4 API
        if (genericApiEntity instanceof io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity) {
            if (
                apiEntity
                    .getListeners()
                    .stream()
                    .anyMatch(listener -> listener instanceof TcpListener)
            ) {
                return EntrypointEntity.Target.TCP;
            }
            if (
                apiEntity
                    .getListeners()
                    .stream()
                    .anyMatch(listener -> listener instanceof HttpListener)
            ) {
                return EntrypointEntity.Target.HTTP;
            }
        }

        throw new EntrypointNotFoundException(genericApiEntity.getId());
    }

    private boolean hasSupportedListeners(GenericApiEntity api) {
        // V1/V2 are always supported (HTTP)
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            log.debug("API [{}] has supported listener", api.getId());
            return true;
        }
        if (api instanceof io.gravitee.rest.api.model.v4.api.ApiEntity v4Api) {
            boolean hasCompatibleListener = v4Api
                .getListeners()
                .stream()
                .anyMatch(listener -> listener instanceof TcpListener || listener instanceof HttpListener);
            if (!hasCompatibleListener) {
                log.debug("API [{}] has no TCP or HTTP listeners — entrypoint checks will be skipped", api.getId());
            }
            return hasCompatibleListener;
        }
        // V4 Native APIs (Kafka) don't need HTTP/TCP
        log.debug("API [{}] is a V4 Native API — skipping HTTP/TCP listener check", api.getId());
        return true;
    }
}

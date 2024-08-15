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
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
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
    // RFC 6454 section-7.1, serialized-origin regex from RFC 3986
    private static final String URI_PATH_SEPARATOR = "/";

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
        String defaultTcpPort = parameterService.find(
            executionContext,
            Key.PORTAL_TCP_PORT,
            executionContext.getEnvironmentId(),
            ParameterReferenceType.ENVIRONMENT
        );

        if (genericApiEntity.getTags() != null && !genericApiEntity.getTags().isEmpty()) {
            List<EntrypointEntity> organizationEntrypoints = entrypointService.findAll(executionContext);

            organizationEntrypoints.forEach(entrypoint -> {
                final String entrypointScheme = getScheme(entrypoint.getValue());
                final String entrypointValue = entrypoint.getValue();
                Set<String> tagEntrypoints = new HashSet<>(Arrays.asList(entrypoint.getTags()));
                tagEntrypoints.retainAll(genericApiEntity.getTags());

                if (tagEntrypoints.size() == entrypoint.getTags().length) {
                    apiEntrypoints.addAll(
                        getEntrypoints(
                            genericApiEntity,
                            entrypointScheme,
                            entrypointValue,
                            defaultTcpPort,
                            tagEntrypoints,
                            executionContext.getEnvironmentId()
                        )
                    );
                }
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
            final String defaultScheme = getScheme(defaultEntrypoint);

            apiEntrypoints.addAll(
                getEntrypoints(
                    genericApiEntity,
                    defaultScheme,
                    defaultEntrypoint,
                    defaultTcpPort,
                    null,
                    executionContext.getEnvironmentId()
                )
            );
        }

        return apiEntrypoints;
    }

    private List<ApiEntrypointEntity> getEntrypoints(
        final GenericApiEntity genericApiEntity,
        final String entrypointScheme,
        final String entrypointHost,
        final String tcpPort,
        final Set<String> tagEntrypoints,
        final String environmentId
    ) {
        if (genericApiEntity.getDefinitionVersion() == DefinitionVersion.FEDERATED) {
            return Collections.emptyList();
        }

        if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity api = (ApiEntity) genericApiEntity;
            return api
                .getProxy()
                .getVirtualHosts()
                .stream()
                .flatMap(virtualHost ->
                    getHttpApiEntrypointEntity(
                        entrypointScheme,
                        entrypointHost,
                        virtualHost.getHost(),
                        virtualHost.getPath(),
                        virtualHost.isOverrideEntrypoint(),
                        tagEntrypoints,
                        environmentId
                    )
                        .stream()
                )
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
                                    entrypointScheme,
                                    entrypointHost,
                                    path.getHost(),
                                    path.getPath(),
                                    path.isOverrideAccess(),
                                    tagEntrypoints,
                                    environmentId
                                )
                                    .stream()
                            );
                    } else if (listener instanceof TcpListener tcpListener) {
                        return tcpListener
                            .getHosts()
                            .stream()
                            .map(tcpHost -> getTcpApiEntrypointEntity(tcpHost, tcpPort, entrypointHost, tagEntrypoints));
                    } else return Stream.empty();
                })
                .toList();
        }
    }

    private List<ApiEntrypointEntity> getHttpApiEntrypointEntity(
        final String defaultScheme,
        final String entrypointValue,
        final String host,
        final String path,
        final boolean isOverride,
        final Set<String> tags,
        final String environmentId
    ) {
        List<ApiEntrypointEntity> entrypoints = new ArrayList<>();

        if (host == null || !isOverride) {
            List<AccessPoint> accessPoints = this.accessPointQueryService.getGatewayAccessPoints(environmentId);
            if (accessPoints.isEmpty()) {
                entrypoints.add(createApiEntrypointEntity(defaultScheme, entrypointValue, path, tags, host));
            } else {
                for (AccessPoint accessPoint : accessPoints) {
                    String targetHost = accessPoint.getHost();
                    String scheme = accessPoint.isSecured() ? "https" : "http";
                    entrypoints.add(createApiEntrypointEntity(scheme, targetHost, path, tags, targetHost));
                }
            }
        } else {
            entrypoints.add(createApiEntrypointEntity(defaultScheme, host, path, tags, host));
        }

        return entrypoints;
    }

    private ApiEntrypointEntity createApiEntrypointEntity(
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

    public String getApiEntrypointsListenerType(GenericApiEntity genericApiEntity) {
        if (
            genericApiEntity.getDefinitionVersion() == DefinitionVersion.V1 ||
            genericApiEntity.getDefinitionVersion() == DefinitionVersion.V2
        ) {
            return "HTTP";
        }
        io.gravitee.rest.api.model.v4.api.ApiEntity api = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
        return api
            .getListeners()
            .stream()
            .findFirst()
            .map(listener -> listener.getType().toString())
            .orElseThrow(() -> new EntrypointNotFoundException(api.getId()));
    }
}

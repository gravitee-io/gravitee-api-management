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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.EntrypointEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.EntrypointService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    public ApiEntrypointServiceImpl(final ParameterService parameterService, final EntrypointService entrypointService) {
        this.parameterService = parameterService;
        this.entrypointService = entrypointService;
    }

    @Override
    public List<ApiEntrypointEntity> getApiEntrypoints(final ExecutionContext executionContext, final GenericApiEntity genericApiEntity) {
        List<ApiEntrypointEntity> apiEntrypoints = new ArrayList<>();

        if (genericApiEntity.getTags() != null && !genericApiEntity.getTags().isEmpty()) {
            List<EntrypointEntity> organizationEntrypoints = entrypointService.findAll(executionContext);
            organizationEntrypoints.forEach(entrypoint -> {
                final String entrypointScheme = getScheme(entrypoint.getValue());
                final String entrypointValue = entrypoint.getValue();
                Set<String> tagEntrypoints = new HashSet<>(Arrays.asList(entrypoint.getTags()));
                tagEntrypoints.retainAll(genericApiEntity.getTags());

                if (tagEntrypoints.size() == entrypoint.getTags().length) {
                    apiEntrypoints.addAll(getEntrypoints(genericApiEntity, entrypointScheme, entrypointValue, tagEntrypoints));
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

            apiEntrypoints.addAll(getEntrypoints(genericApiEntity, defaultScheme, defaultEntrypoint, null));
        }

        return apiEntrypoints;
    }

    private List<ApiEntrypointEntity> getEntrypoints(
        final GenericApiEntity genericApiEntity,
        final String entrypointScheme,
        final String entrypointHost,
        final Set<String> tagEntrypoints
    ) {
        if (genericApiEntity.getDefinitionVersion() == DefinitionVersion.FEDERATED) {
            FederatedApiEntity federatedApiEntity = (FederatedApiEntity) genericApiEntity;
            return List.of(new ApiEntrypointEntity(null, federatedApiEntity.getAccessPoint(), null));
        } else if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity api = (ApiEntity) genericApiEntity;
            return api
                .getProxy()
                .getVirtualHosts()
                .stream()
                .map(virtualHost ->
                    getApiEntrypointEntity(
                        entrypointScheme,
                        entrypointHost,
                        virtualHost.getHost(),
                        virtualHost.getPath(),
                        virtualHost.isOverrideEntrypoint(),
                        tagEntrypoints
                    )
                )
                .collect(Collectors.toList());
        } else {
            io.gravitee.rest.api.model.v4.api.ApiEntity api = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
            return api
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .flatMap(listener -> {
                    HttpListener httpListener = (HttpListener) listener;
                    return httpListener.getPaths().stream();
                })
                .map(path ->
                    getApiEntrypointEntity(
                        entrypointScheme,
                        entrypointHost,
                        path.getHost(),
                        path.getPath(),
                        path.isOverrideAccess(),
                        tagEntrypoints
                    )
                )
                .collect(Collectors.toList());
        }
    }

    private ApiEntrypointEntity getApiEntrypointEntity(
        final String defaultScheme,
        final String entrypointValue,
        final String host,
        final String path,
        final boolean isOverride,
        final Set<String> tags
    ) {
        String targetHost = (host == null || !isOverride) ? entrypointValue : host;
        if (!targetHost.toLowerCase().startsWith("http")) {
            targetHost = defaultScheme + "://" + targetHost;
        }
        return new ApiEntrypointEntity(
            tags,
            DUPLICATE_SLASH_REMOVER.matcher(targetHost + URI_PATH_SEPARATOR + path).replaceAll(URI_PATH_SEPARATOR),
            host
        );
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
}

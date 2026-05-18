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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pure helpers for extracting the HTTP paths an API exposes and for detecting prefix-style conflicts between them.
 * Used by both {@link ApiPathIndex} (snapshot scan) and {@code ApiPathIndexationObserver} (per-event extraction);
 * stateless so it is safe to share across threads.
 */
public final class ApiPathExtractor {

    private ApiPathExtractor() {}

    /**
     * Extracts the listener paths for an api regardless of definition version. Returns an empty list for non-HTTP
     * APIs (federated, native v4) and for v2 APIs with a {@code null} proxy.
     */
    public static List<Path> extractPaths(Api api) {
        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api v4Api -> extractPathsFromV4Listeners(v4Api.getListeners());
            case io.gravitee.definition.model.Api v2Api -> v2Api.getProxy() == null
                ? List.of()
                : extractPathsFromVirtualHosts(v2Api.getProxy().getVirtualHosts());
            case null, default -> new ArrayList<>();
        };
    }

    /** Sanitises and projects each {@link VirtualHost} as a {@link Path}. Used for v2 APIs and legacy v3 ApiEntity. */
    public static List<Path> extractPathsFromVirtualHosts(List<VirtualHost> virtualHosts) {
        if (virtualHosts == null) {
            return List.of();
        }
        return virtualHosts
            .stream()
            .map(virtualHost ->
                Path.builder()
                    .host(virtualHost.getHost())
                    .path(virtualHost.getPath())
                    .overrideAccess(virtualHost.isOverrideEntrypoint())
                    .build()
                    .sanitize()
            )
            .toList();
    }

    /**
     * Sanitises and projects each {@link HttpListener} path as a {@link Path}. Non-HTTP listeners in the list (e.g.,
     * Kafka, TCP) are skipped.
     */
    public static List<Path> extractPathsFromV4Listeners(List<Listener> listeners) {
        if (listeners == null) {
            return List.of();
        }
        return listeners
            .stream()
            .flatMap(listener -> listener instanceof HttpListener httpListener ? Stream.of(httpListener) : Stream.of())
            .flatMap(httpListener ->
                httpListener
                    .getPaths()
                    .stream()
                    .map(path ->
                        Path.builder().host(path.getHost()).path(path.getPath()).overrideAccess(path.isOverrideAccess()).build().sanitize()
                    )
            )
            .toList();
    }

    /**
     * Returns the first conflict between {@code path} and an entry of {@code existingPaths}. A conflict is a
     * containment relationship (either string starts with the other) — so {@code /foo} conflicts with {@code /foo/bar}
     * and vice versa. The error message reports the existing path (the one already published).
     */
    public static Optional<Validator.Error> findConflictingPathError(String path, List<String> existingPaths) {
        return existingPaths
            .stream()
            .filter(existingPath -> existingPath.startsWith(path) || path.startsWith(existingPath))
            .findFirst()
            .map(conflictingPath -> Validator.Error.severe("Path [%s] already exists", conflictingPath));
    }

    /** Groups paths by their {@code host} value, dropping paths without a host. The result is keyed by host. */
    public static Map<String, List<String>> getPathsWithHost(List<Path> paths) {
        return paths
            .stream()
            .filter(path -> StringUtils.isNotEmpty(path.getHost()))
            .collect(Collectors.groupingBy(Path::getHost, Collectors.mapping(Path::getPath, Collectors.toList())));
    }

    /** Returns the path strings whose {@code host} is empty, preserving order. */
    public static List<String> getPathsWithoutHost(List<Path> paths) {
        return paths
            .stream()
            .filter(path -> StringUtils.isEmpty(path.getHost()))
            .map(Path::getPath)
            .toList();
    }
}

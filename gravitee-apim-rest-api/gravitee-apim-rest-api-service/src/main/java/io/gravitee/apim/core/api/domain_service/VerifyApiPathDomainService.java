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
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.exception.InvalidPathsException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerifyApiPathDomainService {

    private final EnvironmentCrudService environmentCrudService;
    private final ApiQueryService apiSearchService;
    private final ApiDefinitionParserDomainService apiDefinitionParserDomainService;
    private final ApiHostValidatorDomainService apiHostValidatorDomainService;

    public VerifyApiPathDomainService(
        final EnvironmentCrudService environmentCrudService,
        final ApiQueryService apiSearchService,
        final ApiDefinitionParserDomainService apiDefinitionParserDomainService,
        final ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        this.environmentCrudService = environmentCrudService;
        this.apiSearchService = apiSearchService;
        this.apiDefinitionParserDomainService = apiDefinitionParserDomainService;
        this.apiHostValidatorDomainService = apiHostValidatorDomainService;
    }

    public List<Path> checkAndSanitizeApiPaths(String environmentId, String apiId, List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new InvalidPathsException("At least one path is required for the listener HTTP.");
        }

        try {
            List<Path> sanitizedPaths = paths
                .stream()
                .map(path ->
                    Path.builder().host(path.getHost()).path(path.getPath()).overrideAccess(path.isOverrideAccess()).build().sanitize()
                )
                .toList();

            List<Path> pathsWithDomain = validateAndSetDomain(environmentId, sanitizedPaths);

            checkNoDuplicate(pathsWithDomain);

            checkPathsAreAvailable(environmentId, apiId, pathsWithDomain);

            return pathsWithDomain;
        } catch (PathAlreadyExistsException | InvalidHostException e) {
            throw new InvalidPathsException(e.getMessage(), e);
        }
    }

    private void checkNoDuplicate(List<Path> paths) throws PathAlreadyExistsException {
        var set = new HashSet<>();
        var duplicates = paths.stream().filter(n -> !set.add(n)).toList();

        if (!duplicates.isEmpty()) {
            throw new PathAlreadyExistsException(duplicates.get(0).toString());
        }
    }

    private void checkPathsAreAvailable(String environmentId, String apiId, List<Path> paths) {
        apiSearchService
            .search(
                ApiSearchCriteria.builder().environmentId(environmentId).build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).build()
            )
            .filter(api -> !api.getId().equals(apiId))
            .map(this::extractPaths)
            .filter(extractedPaths -> extractedPaths != null && !extractedPaths.isEmpty())
            .forEach(existingPaths -> {
                // Extract all paths with a host
                Map<String, List<String>> registeredPathWithHosts = existingPaths
                    .stream()
                    .filter(path -> path.getHost() != null && !path.getHost().isEmpty())
                    .collect(Collectors.groupingBy(Path::getHost, Collectors.mapping(Path::getPath, Collectors.toList())));

                // Check only paths with a host and compare to registered virtual hosts
                if (!registeredPathWithHosts.isEmpty()) {
                    paths
                        .stream()
                        .filter(path -> path.getHost() != null && !path.getHost().isEmpty())
                        .forEach(path -> {
                            checkPathNotYetRegistered(path.getPath(), registeredPathWithHosts.get(path.getHost()));
                        });
                }

                // Extract all paths without host
                List<String> registeredPathWithoutHost = existingPaths
                    .stream()
                    .filter(path -> path.getHost() == null || path.getHost().isEmpty())
                    .map(Path::getPath)
                    .collect(Collectors.toList());

                // Then check remaining paths without a host and compare to registered paths without host
                if (!registeredPathWithoutHost.isEmpty()) {
                    paths
                        .stream()
                        .filter(path -> path.getHost() == null || path.getHost().isEmpty())
                        .forEach(virtualHost -> checkPathNotYetRegistered(virtualHost.getPath(), registeredPathWithoutHost));
                }
            });
    }

    private List<Path> extractPaths(final Api api) {
        if (api.getDefinition() != null) {
            if (api.getDefinitionVersion() != null && DefinitionVersion.V4.name().equals(api.getDefinitionVersion().name())) {
                return apiDefinitionParserDomainService
                    .readV4ApiDefinition(api.getDefinition())
                    .getListeners()
                    .stream()
                    .filter(HttpListener.class::isInstance)
                    .map(HttpListener.class::cast)
                    .flatMap(httpListener ->
                        httpListener
                            .getPaths()
                            .stream()
                            .map(path ->
                                Path
                                    .builder()
                                    .host(path.getHost())
                                    .path(path.getPath())
                                    .overrideAccess(path.isOverrideAccess())
                                    .build()
                                    .sanitize()
                            )
                    )
                    .collect(Collectors.toList());
            } else {
                return apiDefinitionParserDomainService
                    .readV2ApiDefinition(api.getDefinition())
                    .getProxy()
                    .getVirtualHosts()
                    .stream()
                    .map(virtualHost ->
                        Path
                            .builder()
                            .host(virtualHost.getHost())
                            .path(virtualHost.getPath())
                            .overrideAccess(virtualHost.isOverrideEntrypoint())
                            .build()
                            .sanitize()
                    )
                    .collect(Collectors.toList());
            }
        }
        return null;
    }

    private void checkPathNotYetRegistered(final String path, final List<String> registeredPaths) {
        boolean match =
            registeredPaths != null &&
            registeredPaths.stream().anyMatch(registeredPath -> path.startsWith(registeredPath) || registeredPath.startsWith(path));

        if (match) {
            throw new PathAlreadyExistsException(path);
        }
    }

    private List<Path> validateAndSetDomain(String environmentId, List<Path> sanitizedPaths) throws InvalidPathsException {
        var env = environmentCrudService.get(environmentId);

        var domainRestrictions = env.getDomainRestrictions();
        if (domainRestrictions != null && !domainRestrictions.isEmpty()) {
            return sanitizedPaths
                .stream()
                .map(path -> {
                    if (path.hasHost()) {
                        checkDomainIsValid(path, domainRestrictions);
                        return path;
                    }

                    return path.withHost(env.getDomainRestrictions().get(0));
                })
                .collect(Collectors.toList());
        }

        return sanitizedPaths;
    }

    private void checkDomainIsValid(Path path, List<String> domainRestrictions) {
        String hostWithoutPort = path.getHost().split(":")[0];
        if (!this.apiHostValidatorDomainService.isValidDomainOrSubDomain(hostWithoutPort, domainRestrictions)) {
            throw new InvalidHostException("Host [" + hostWithoutPort + "] must be a subdomain of " + domainRestrictions);
        }
    }
}

class InvalidHostException extends RuntimeException {

    public InvalidHostException(String message) {
        super(message);
    }
}

class PathAlreadyExistsException extends RuntimeException {

    public PathAlreadyExistsException(String path) {
        super("Path [" + path + "] already exists");
    }
}

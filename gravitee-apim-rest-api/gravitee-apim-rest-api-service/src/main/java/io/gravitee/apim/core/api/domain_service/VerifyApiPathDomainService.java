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

import io.gravitee.apim.core.api.exception.InvalidHostException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerifyApiPathDomainService {

    private final ApiQueryService apiSearchService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final ApiHostValidatorDomainService apiHostValidatorDomainService;

    public VerifyApiPathDomainService(
        final ApiQueryService apiSearchService,
        final InstallationAccessQueryService installationAccessQueryService,
        final ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        this.apiSearchService = apiSearchService;
        this.installationAccessQueryService = installationAccessQueryService;
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
                ApiSearchCriteria
                    .builder()
                    .definitionVersion(List.of(DefinitionVersion.V1, DefinitionVersion.V2, DefinitionVersion.V4))
                    .environmentId(environmentId)
                    .build(),
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
                        .forEach(path -> checkPathNotYetRegistered(path.getPath(), registeredPathWithHosts.get(path.getHost())));
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
        if (api.getDefinitionVersion() != null && api.getDefinitionVersion() == DefinitionVersion.V4) {
            return api
                .getApiDefinitionV4()
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
            return api
                .getApiDefinition()
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

    private void checkPathNotYetRegistered(final String path, final List<String> registeredPaths) {
        boolean match =
            registeredPaths != null &&
            registeredPaths.stream().anyMatch(registeredPath -> path.startsWith(registeredPath) || registeredPath.startsWith(path));

        if (match) {
            throw new PathAlreadyExistsException(path);
        }
    }

    private List<Path> validateAndSetDomain(String environmentId, List<Path> sanitizedPaths) throws InvalidPathsException {
        List<RestrictedDomain> restrictedDomains = installationAccessQueryService.getGatewayRestrictedDomains(environmentId);
        if (restrictedDomains != null && !restrictedDomains.isEmpty()) {
            for (Path path : sanitizedPaths) {
                if (path.hasHost()) {
                    checkDomainIsValid(path, restrictedDomains);
                } else {
                    path.setHost(restrictedDomains.get(0).getDomain());
                }
            }
            if (!sanitizedPaths.isEmpty() && sanitizedPaths.stream().noneMatch(Path::isOverrideAccess)) {
                sanitizedPaths.get(0).setOverrideAccess(true);
            }
        }

        return sanitizedPaths;
    }

    private void checkDomainIsValid(Path path, List<RestrictedDomain> restrictedDomainEntities) {
        String hostWithoutPort = extractHost(path.getHost());
        List<String> restrictedDomainsWithoutPort = restrictedDomainEntities
            .stream()
            .map(restrictedDomainEntity -> extractHost(restrictedDomainEntity.getDomain()))
            .toList();
        String onlyPort = extractPort(path.getHost());
        if (
            !this.apiHostValidatorDomainService.isValidDomainOrSubDomain(hostWithoutPort, restrictedDomainsWithoutPort) ||
            !isValidPort(onlyPort, restrictedDomainEntities)
        ) {
            throw new InvalidHostException(
                "Host [" +
                hostWithoutPort +
                "] must be a subdomain of " +
                restrictedDomainEntities.stream().map(RestrictedDomain::getDomain).toList()
            );
        }
    }

    private boolean isValidPort(final String port, final List<RestrictedDomain> restrictedDomainEntities) {
        if (restrictedDomainEntities.isEmpty()) {
            return true;
        }
        return restrictedDomainEntities
            .stream()
            .anyMatch(restrictedDomainEntity -> {
                String domainRestriction = restrictedDomainEntity.getDomain();
                String domainRestrictionOnlyPort = extractPort(domainRestriction);
                return (
                    (port == null && domainRestrictionOnlyPort == null) ||
                    (port == null && isDefaultHttp(domainRestrictionOnlyPort)) ||
                    (domainRestrictionOnlyPort == null && isDefaultHttp(port)) ||
                    Objects.equals(port, domainRestrictionOnlyPort)
                );
            });
    }

    private static boolean isDefaultHttp(final String domainRestrictionOnlyPort) {
        return "80".equals(domainRestrictionOnlyPort) || "443".equals(domainRestrictionOnlyPort);
    }

    private static String extractHost(final String hostAndPort) {
        return hostAndPort.split(":")[0];
    }

    private static String extractPort(final String hostAndPort) {
        String[] split = hostAndPort.split(":");
        if (split.length > 1) {
            return split[1];
        }
        return null;
    }
}

class PathAlreadyExistsException extends RuntimeException {

    public PathAlreadyExistsException(String path) {
        super("Path [" + path + "] already exists");
    }
}

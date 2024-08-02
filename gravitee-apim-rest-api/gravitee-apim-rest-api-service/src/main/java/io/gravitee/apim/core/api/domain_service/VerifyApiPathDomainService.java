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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Path;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.installation.model.RestrictedDomain;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
public class VerifyApiPathDomainService implements Validator<VerifyApiPathDomainService.Input> {

    public record Input(String environmentId, String apiId, List<Path> paths) implements Validator.Input {}

    private final ApiQueryService apiSearchService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final ApiHostValidatorDomainService apiHostValidatorDomainService;

    public VerifyApiPathDomainService(
        ApiQueryService apiSearchService,
        InstallationAccessQueryService installationAccessQueryService,
        ApiHostValidatorDomainService apiHostValidatorDomainService
    ) {
        this.apiSearchService = apiSearchService;
        this.installationAccessQueryService = installationAccessQueryService;
        this.apiHostValidatorDomainService = apiHostValidatorDomainService;
    }

    @Override
    public Result<VerifyApiPathDomainService.Input> validateAndSanitize(VerifyApiPathDomainService.Input input) {
        if (CollectionUtils.isEmpty(input.paths)) {
            return Result.ofErrors(List.of(Error.severe("HTTP listener requires a minimum of one path")));
        }

        var sanitizedBuilder = input.paths.stream().map(Path::toBuilder).toList();

        var errors = new ArrayList<Error>();

        errors.addAll(invalidPathErrors(sanitizedBuilder));

        errors.addAll(duplicatePathErrors(sanitizedBuilder));

        errors.addAll(invalidDomainErrors(input.environmentId, sanitizedBuilder));

        errors.addAll(unavailablePathErrors(input, sanitizedBuilder));

        var sanitized = sanitizedBuilder.stream().map(Path.PathBuilder::build).toList();

        return Result.ofBoth(new Input(input.environmentId, input.apiId, sanitized), errors);
    }

    private List<Error> invalidDomainErrors(String environmentId, List<Path.PathBuilder> sanitizedBuilder) throws InvalidPathsException {
        var errors = new ArrayList<Error>();

        var restrictedDomains = installationAccessQueryService.getGatewayRestrictedDomains(environmentId);

        if (CollectionUtils.isNotEmpty(restrictedDomains)) {
            for (var builder : sanitizedBuilder) {
                if (builder.build().hasHost()) {
                    invalidDomainError(builder.build(), restrictedDomains).ifPresent(errors::add);
                }
            }
            if (sanitizedBuilder.stream().noneMatch(builder -> builder.build().isOverrideAccess())) {
                sanitizedBuilder.stream().findFirst().ifPresent(builder -> builder.overrideAccess(true));
            }
        }

        return errors;
    }

    private Optional<Validator.Error> invalidDomainError(Path path, List<RestrictedDomain> restrictedDomains) {
        var hostWithoutPort = extractHost(path.getHost());
        var hostPort = extractPort(path.getHost());

        var restrictedDomainsWithoutPort = restrictedDomains
            .stream()
            .map(restrictedDomainEntity -> extractHost(restrictedDomainEntity.getDomain()))
            .toList();

        var isValidDomain = apiHostValidatorDomainService.isValidDomainOrSubDomain(hostWithoutPort, restrictedDomainsWithoutPort);

        var isValidPort = isValidPort(hostPort, restrictedDomains);

        if (!isValidDomain || !isValidPort) {
            return Optional.of(Validator.Error.severe("Domain [%s] is invalid", path.getHost()));
        }
        return Optional.empty();
    }

    private List<Error> unavailablePathErrors(Input input, List<Path.PathBuilder> sanitizedBuilder) {
        var errors = new ArrayList<Error>();

        apiSearchService
            .search(
                ApiSearchCriteria
                    .builder()
                    .environmentId(input.environmentId)
                    .definitionVersion(List.of(DefinitionVersion.V2, DefinitionVersion.V4))
                    .build(),
                null,
                ApiFieldFilter.builder().pictureExcluded(true).build()
            )
            .filter(api -> !api.getId().equals(input.apiId))
            .map(VerifyApiPathDomainService::extractPaths)
            .filter(CollectionUtils::isNotEmpty)
            .forEach(existingPaths -> {
                var paths = sanitizedBuilder.stream().map(Path.PathBuilder::build).toList();
                var pathsWithHost = getPathsWithHost(paths);
                var existingPathsWithHost = getPathsWithHost(existingPaths);
                pathsWithHost.forEach((host, hostPaths) ->
                    hostPaths.forEach(hostPath -> findConflictingPathError(hostPath, existingPathsWithHost.get(host)).ifPresent(errors::add)
                    )
                );

                var pathsWithoutHosts = getPathsWithoutHost(paths);
                var existingPathsWithoutHost = getPathsWithoutHost(existingPaths);
                pathsWithoutHosts.forEach(path -> findConflictingPathError(path, existingPathsWithoutHost).ifPresent(errors::add));
            });

        return errors;
    }

    private Optional<Error> findConflictingPathError(String path, List<String> existingPaths) {
        return existingPaths
            .stream()
            .findFirst()
            .filter(existingPath -> existingPath.startsWith(path) || path.startsWith(existingPath))
            .map(conflictingPath -> Error.severe("Path [%s] already exists", conflictingPath));
    }

    private List<Error> invalidPathErrors(List<Path.PathBuilder> sanitizedBuilder) {
        var errors = new ArrayList<Error>();

        sanitizedBuilder.forEach(builder -> {
            var path = builder.build();
            try {
                var sanitized = path.sanitize();
                builder.path(sanitized.getPath());
            } catch (InvalidPathsException e) {
                errors.add(Error.severe("Path [%s] is invalid", path.getPath()));
            }
        });

        return errors;
    }

    private List<Error> duplicatePathErrors(List<Path.PathBuilder> sanitizedBuilder) {
        var seen = new HashSet<>();
        return sanitizedBuilder
            .stream()
            .map(Path.PathBuilder::build)
            .filter(path -> !seen.add(path))
            .map(duplicate -> Error.severe("Path [%s] is duplicated", duplicate.getPath()))
            .toList();
    }

    private static Map<String, List<String>> getPathsWithHost(List<Path> paths) {
        return paths
            .stream()
            .filter(path -> StringUtils.isNotEmpty(path.getHost()))
            .collect(Collectors.groupingBy(Path::getHost, Collectors.mapping(Path::getPath, Collectors.toList())));
    }

    private static List<String> getPathsWithoutHost(List<Path> paths) {
        return paths.stream().filter(path -> StringUtils.isEmpty(path.getHost())).map(Path::getPath).toList();
    }

    private static List<Path> extractPaths(Api api) {
        return api.getDefinitionVersion() == DefinitionVersion.V4 ? getV4Paths(api) : getV2Paths(api);
    }

    private static List<Path> getV4Paths(Api api) {
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
                        Path.builder().host(path.getHost()).path(path.getPath()).overrideAccess(path.isOverrideAccess()).build().sanitize()
                    )
            )
            .toList();
    }

    private static List<Path> getV2Paths(Api api) {
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
            .toList();
    }

    private static boolean isValidPort(final String port, final List<RestrictedDomain> restrictedDomainEntities) {
        return restrictedDomainEntities
            .stream()
            .anyMatch(restrictedDomainEntity -> {
                String restrictedDomain = restrictedDomainEntity.getDomain();
                String restrictedPort = extractPort(restrictedDomain);
                return isValidPort(port, restrictedPort);
            });
    }

    private static boolean isValidPort(String port, String restrictedPort) {
        if (port == null && restrictedPort == null) {
            return true;
        }
        if (port == null && isDefaultHttp(restrictedPort)) {
            return true;
        }
        if (restrictedPort == null && isDefaultHttp(port)) {
            return true;
        }
        return Objects.equals(port, restrictedPort);
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

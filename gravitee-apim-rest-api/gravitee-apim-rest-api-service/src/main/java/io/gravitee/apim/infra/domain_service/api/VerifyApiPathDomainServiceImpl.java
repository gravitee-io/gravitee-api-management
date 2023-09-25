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
package io.gravitee.apim.infra.domain_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.model.*;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.exception.HttpListenerPathMissingException;
import io.gravitee.rest.api.service.v4.exception.InvalidHostException;
import io.gravitee.rest.api.service.v4.exception.PathAlreadyExistsException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VerifyApiPathDomainServiceImpl implements VerifyApiPathDomainService {

    private final EnvironmentCrudService environmentCrudService;
    private final ApiQueryService apiSearchService;
    private final ObjectMapper objectMapper;

    public VerifyApiPathDomainServiceImpl(
        @Lazy final EnvironmentCrudService environmentCrudService,
        @Lazy final ApiQueryService apiSearchService,
        final ObjectMapper objectMapper
    ) {
        this.environmentCrudService = environmentCrudService;
        this.apiSearchService = apiSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Path> verifyApiPaths(ExecutionContext executionContext, String apiId, List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new HttpListenerPathMissingException();
        }
        List<Path> sanitizedPaths = paths
            .stream()
            .map(path -> Path.builder().host(path.getHost()).path(path.getPath()).build().sanitize())
            .toList();

        List<Path> pathsWithDomain = validateAndSetDomain(executionContext.getEnvironmentId(), sanitizedPaths);

        checkNoDuplicate(pathsWithDomain);

        checkPathsAreAvailable(executionContext.getEnvironmentId(), apiId, pathsWithDomain);

        return pathsWithDomain;
    }

    private void checkNoDuplicate(List<Path> paths) {
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
                    .filter(path -> !Strings.isNullOrEmpty(path.getHost()))
                    .collect(Collectors.groupingBy(Path::getHost, Collectors.mapping(Path::getPath, Collectors.toList())));

                // Check only paths with a host and compare to registered virtual hosts
                if (!registeredPathWithHosts.isEmpty()) {
                    paths
                        .stream()
                        .filter(path -> !Strings.isNullOrEmpty(path.getHost()))
                        .forEach(path -> checkPathNotYetRegistered(path.getPath(), registeredPathWithHosts.get(path.getHost())));
                }

                // Extract all paths without host
                List<String> registeredPathWithoutHost = existingPaths
                    .stream()
                    .filter(path -> Strings.isNullOrEmpty(path.getHost()))
                    .map(Path::getPath)
                    .collect(Collectors.toList());

                // Then check remaining paths without a host and compare to registered paths without host
                if (!registeredPathWithoutHost.isEmpty()) {
                    paths
                        .stream()
                        .filter(path -> Strings.isNullOrEmpty(path.getHost()))
                        .forEach(virtualHost -> checkPathNotYetRegistered(virtualHost.getPath(), registeredPathWithoutHost));
                }
            });
    }

    private List<Path> extractPaths(final Api api) {
        if (api.getDefinition() != null) {
            if (api.getDefinitionVersion() != null && DefinitionVersion.V4.name().equals(api.getDefinitionVersion().name())) {
                try {
                    io.gravitee.definition.model.v4.Api apiDefinition = objectMapper.readValue(
                        api.getDefinition(),
                        io.gravitee.definition.model.v4.Api.class
                    );
                    return apiDefinition
                        .getListeners()
                        .stream()
                        .filter(HttpListener.class::isInstance)
                        .map(HttpListener.class::cast)
                        .flatMap(httpListener ->
                            httpListener
                                .getPaths()
                                .stream()
                                .map(path -> Path.builder().host(path.getHost()).path(path.getPath()).build().sanitize())
                        )
                        .collect(Collectors.toList());
                } catch (IOException ioe) {
                    log.error("Unexpected error while getting API definition", ioe);
                }
            } else {
                try {
                    io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                        api.getDefinition(),
                        io.gravitee.definition.model.Api.class
                    );
                    return apiDefinition
                        .getProxy()
                        .getVirtualHosts()
                        .stream()
                        .map(virtualHost -> Path.builder().host(virtualHost.getHost()).path(virtualHost.getPath()).build().sanitize())
                        .collect(Collectors.toList());
                } catch (IOException ioe) {
                    log.error("Unexpected error while getting API definition", ioe);
                }
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

    private List<Path> validateAndSetDomain(String environmentId, List<Path> sanitizedPaths) {
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
        if (!isValidDomainOrSubDomain(hostWithoutPort, domainRestrictions)) {
            throw new InvalidHostException(hostWithoutPort, domainRestrictions);
        }
    }

    private boolean isValidDomainOrSubDomain(String domain, List<String> domainRestrictions) {
        boolean isSubDomain = false;
        if (domainRestrictions.isEmpty()) {
            return true;
        }
        for (String domainRestriction : domainRestrictions) {
            InternetDomainName domainIDN = InternetDomainName.from(domain);
            InternetDomainName parentIDN = InternetDomainName.from(domainRestriction);

            if (domainIDN.equals(parentIDN)) {
                return true;
            }
            while (!isSubDomain && domainIDN.hasParent()) {
                isSubDomain = parentIDN.equals(domainIDN);
                domainIDN = domainIDN.parent();
            }
            if (isSubDomain) {
                break;
            }
        }
        return isSubDomain;
    }
}

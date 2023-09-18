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
package io.gravitee.rest.api.service.v4.impl.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.net.InternetDomainName;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.RestrictedDomainEntity;
import io.gravitee.rest.api.service.AccessPointService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.exception.HttpListenerPathMissingException;
import io.gravitee.rest.api.service.v4.exception.InvalidHostException;
import io.gravitee.rest.api.service.v4.exception.PathAlreadyExistsException;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class PathValidationServiceImpl implements PathValidationService {

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("[//]+");
    private static final String URI_PATH_SEPARATOR = "/";
    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    private final ApiRepository apiRepository;
    private final ObjectMapper objectMapper;
    private final AccessPointService accessPointService;

    public PathValidationServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ObjectMapper objectMapper,
        final AccessPointService accessPointService
    ) {
        this.apiRepository = apiRepository;
        this.objectMapper = objectMapper;
        this.accessPointService = accessPointService;
    }

    @Override
    public List<Path> validateAndSanitizePaths(final ExecutionContext executionContext, final String apiId, final List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            throw new HttpListenerPathMissingException();
        }
        List<Path> sanitizedPaths = paths
            .stream()
            .map(path -> new Path(path.getHost(), sanitizePath(path.getPath()), path.isOverrideAccess()))
            .toList();

        // validate domain restrictions
        validateDomainRestrictions(executionContext, sanitizedPaths);

        // Get all the paths declared on all API of the currentEnvironment, except the one to update
        apiRepository
            .search(
                new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
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
                    sanitizedPaths
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
                    sanitizedPaths
                        .stream()
                        .filter(path -> Strings.isNullOrEmpty(path.getHost()))
                        .forEach(virtualHost -> checkPathNotYetRegistered(virtualHost.getPath(), registeredPathWithoutHost));
                }
            });

        return sanitizedPaths;
    }

    private void checkPathNotYetRegistered(final String path, final List<String> registeredPaths) {
        boolean match =
            registeredPaths != null &&
            registeredPaths.stream().anyMatch(registeredPath -> path.startsWith(registeredPath) || registeredPath.startsWith(path));

        if (match) {
            throw new PathAlreadyExistsException(path);
        }
    }

    private List<Path> extractPaths(final Api api) {
        if (api.getDefinition() != null) {
            if (api.getDefinitionVersion() == DefinitionVersion.V4) {
                try {
                    io.gravitee.definition.model.v4.Api apiDefinition = objectMapper.readValue(
                        api.getDefinition(),
                        io.gravitee.definition.model.v4.Api.class
                    );
                    return apiDefinition
                        .getListeners()
                        .stream()
                        .filter(listener -> listener instanceof HttpListener)
                        .map(listener -> (HttpListener) listener)
                        .flatMap(httpListener ->
                            httpListener.getPaths().stream().map(path -> new Path(path.getHost(), sanitizePath(path.getPath())))
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
                        .map(virtualHost -> new Path(virtualHost.getHost(), sanitizePath(virtualHost.getPath())))
                        .collect(Collectors.toList());
                } catch (IOException ioe) {
                    log.error("Unexpected error while getting API definition", ioe);
                }
            }
        }
        return null;
    }

    @Override
    public String sanitizePath(final String path) {
        String sanitizedPath = path;
        if (sanitizedPath == null || sanitizedPath.isEmpty()) {
            sanitizedPath = URI_PATH_SEPARATOR;
        }

        if (!sanitizedPath.startsWith(URI_PATH_SEPARATOR)) {
            sanitizedPath = URI_PATH_SEPARATOR + sanitizedPath;
        }

        if (sanitizedPath.lastIndexOf(URI_PATH_SEPARATOR_CHAR) != sanitizedPath.length() - 1) {
            sanitizedPath += URI_PATH_SEPARATOR;
        }

        return DUPLICATE_SLASH_REMOVER.matcher(sanitizedPath).replaceAll(URI_PATH_SEPARATOR);
    }

    private void validateDomainRestrictions(final ExecutionContext executionContext, final List<Path> paths) {
        final List<RestrictedDomainEntity> restrictedDomains = accessPointService.getGatewayRestrictedDomains(
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId()
        );
        if (restrictedDomains != null && !restrictedDomains.isEmpty()) {
            for (Path path : paths) {
                String host = path.getHost();
                if (!StringUtils.isEmpty(host)) {
                    String hostWithoutPort = host.split(":")[0];
                    if (!isValidDomainOrSubDomain(hostWithoutPort, restrictedDomains)) {
                        throw new InvalidHostException(
                            hostWithoutPort,
                            restrictedDomains.stream().map(RestrictedDomainEntity::getDomain).toList()
                        );
                    }
                } else {
                    path.setHost(restrictedDomains.get(0).getDomain());
                }
            }
            if (!paths.isEmpty() && paths.stream().noneMatch(Path::isOverrideAccess)) {
                paths.get(0).setOverrideAccess(true);
            }
        }
    }

    private boolean isValidDomainOrSubDomain(String domain, List<RestrictedDomainEntity> restrictedDomainEntities) {
        boolean isSubDomain = false;
        if (restrictedDomainEntities.isEmpty()) {
            return true;
        }
        for (RestrictedDomainEntity restrictedDomainEntity : restrictedDomainEntities) {
            String domainRestriction = restrictedDomainEntity.getDomain();
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

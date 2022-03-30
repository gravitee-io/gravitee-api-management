/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.InternetDomainName;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidVirtualHostException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class VirtualHostServiceImpl extends TransactionalService implements VirtualHostService {

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualHostServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnvironmentService environmentService;

    @Override
    public Collection<VirtualHost> sanitizeAndValidate(
        ExecutionContext executionContext,
        Collection<VirtualHost> virtualHosts,
        String apiId
    ) {
        // Sanitize virtual hosts
        Collection<VirtualHost> sanitizedVirtualHosts = virtualHosts.stream().map(this::sanitize).collect(Collectors.toList());

        // validate domain restrictions
        validateDomainRestrictions(executionContext, sanitizedVirtualHosts);

        // Get all the API of the currentEnvironment, except the one to update
        Set<ApiEntity> apis = apiRepository
            .search(null)
            .stream()
            .filter(api -> !api.getId().equals(apiId) && api.getEnvironmentId().equals(executionContext.getEnvironmentId()))
            .map(this::convert)
            .collect(Collectors.toSet());

        // Extract all the virtual hosts with a host
        Map<String, List<String>> registeredVirtualHosts = apis
            .stream()
            .flatMap(
                (Function<ApiEntity, Stream<VirtualHost>>) api ->
                    api
                        .getProxy()
                        .getVirtualHosts()
                        .stream()
                        .filter(virtualHost -> virtualHost.getHost() != null && !virtualHost.getHost().isEmpty())
            )
            .collect(Collectors.groupingBy(VirtualHost::getHost, Collectors.mapping(VirtualHost::getPath, Collectors.toList())));

        // Extract all the virtual hosts with a single path
        List<String> registeredContextPaths = apis
            .stream()
            .flatMap(
                (Function<ApiEntity, Stream<String>>) api ->
                    api.getProxy().getVirtualHosts().stream().filter(virtualHost -> virtualHost.getHost() == null).map(VirtualHost::getPath)
            )
            .collect(Collectors.toList());

        // Check only virtual hosts with a host and compare to registered virtual hosts
        if (!registeredVirtualHosts.isEmpty()) {
            sanitizedVirtualHosts
                .stream()
                .filter(virtualHost -> virtualHost.getHost() != null && !virtualHost.getHost().isEmpty())
                .forEach(
                    virtualHost -> checkPathNotYetRegistered(virtualHost.getPath(), registeredVirtualHosts.get(virtualHost.getHost()))
                );
        }

        // Then check remaining virtual hosts without a host and compare to registered context paths
        if (!registeredContextPaths.isEmpty()) {
            sanitizedVirtualHosts
                .stream()
                .filter(virtualHost -> virtualHost.getHost() == null)
                .forEach(virtualHost -> checkPathNotYetRegistered(virtualHost.getPath(), registeredContextPaths));
        }
        return sanitizedVirtualHosts;
    }

    private void validateDomainRestrictions(ExecutionContext executionContext, Collection<VirtualHost> virtualHosts) {
        final EnvironmentEntity currentEnv = environmentService.findById(executionContext.getEnvironmentId());
        final List<String> domainRestrictions = currentEnv.getDomainRestrictions();
        if (domainRestrictions != null && !domainRestrictions.isEmpty()) {
            for (VirtualHost vHost : virtualHosts) {
                String host = vHost.getHost();
                if (!StringUtils.isEmpty(host)) {
                    String hostWithoutPort = host.split(":")[0];
                    if (!isValidDomainOrSubDomain(hostWithoutPort, domainRestrictions)) {
                        throw new InvalidVirtualHostException(hostWithoutPort, domainRestrictions);
                    }
                } else {
                    vHost.setHost(domainRestrictions.get(0));
                }
            }
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

    @Override
    public VirtualHost sanitize(VirtualHost virtualHost) {
        String path = virtualHost.getPath();
        if (path == null || path.isEmpty()) {
            path = URI_PATH_SEPARATOR;
        }

        if (!path.startsWith(URI_PATH_SEPARATOR)) {
            path = URI_PATH_SEPARATOR + path;
        }

        if (path.lastIndexOf(URI_PATH_SEPARATOR_CHAR) != path.length() - 1) {
            path += URI_PATH_SEPARATOR;
        }

        path = DUPLICATE_SLASH_REMOVER.matcher(path).replaceAll(URI_PATH_SEPARATOR);

        // Create a copy of the virtual host to avoid any change into the initial one
        return new VirtualHost(virtualHost.getHost(), path, virtualHost.isOverrideEntrypoint());
    }

    private void checkPathNotYetRegistered(String path, List<String> registeredPaths) {
        boolean match =
            registeredPaths != null &&
            registeredPaths.stream().anyMatch(registeredPath -> path.startsWith(registeredPath) || registeredPath.startsWith(path));

        if (match) {
            throw new ApiContextPathAlreadyExistsException(path);
        }
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(api.getId());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                    api.getDefinition(),
                    io.gravitee.definition.model.Api.class
                );
                apiEntity.setProxy(apiDefinition.getProxy());

                // Sanitize virtual hosts
                apiEntity
                    .getProxy()
                    .setVirtualHosts(apiEntity.getProxy().getVirtualHosts().stream().map(this::sanitize).collect(Collectors.toList()));
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while getting API definition", ioe);
            }
        }

        return apiEntity;
    }
}

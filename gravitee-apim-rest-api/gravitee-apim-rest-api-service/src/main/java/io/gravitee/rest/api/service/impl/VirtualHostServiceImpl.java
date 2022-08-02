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

import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidVirtualHostException;
import io.gravitee.rest.api.service.v4.exception.InvalidHostException;
import io.gravitee.rest.api.service.v4.exception.PathAlreadyExistsException;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class VirtualHostServiceImpl extends TransactionalService implements VirtualHostService {

    private final PathValidationService pathValidationService;

    public VirtualHostServiceImpl(final PathValidationService pathValidationService) {
        this.pathValidationService = pathValidationService;
    }

    @Override
    public Collection<VirtualHost> sanitizeAndValidate(
        ExecutionContext executionContext,
        Collection<VirtualHost> virtualHosts,
        String apiId
    ) {
        // Sanitize virtual hosts
        List<Path> paths = virtualHosts
            .stream()
            .map(virtualHost -> new Path(virtualHost.getHost(), virtualHost.getPath()))
            .collect(Collectors.toList());

        try {
            return pathValidationService
                .validateAndSanitizePaths(executionContext, apiId, paths)
                .stream()
                .map(path -> new VirtualHost(path.getHost(), path.getPath()))
                .collect(Collectors.toList());
        } catch (PathAlreadyExistsException e) {
            throw new ApiContextPathAlreadyExistsException(e.getPathValue());
        } catch (InvalidHostException e) {
            throw new InvalidVirtualHostException(e.getHost(), e.getRestrictions());
        }
    }

    @Override
    public VirtualHost sanitize(VirtualHost virtualHost) {
        String path = virtualHost.getPath();
        String sanitizePath = pathValidationService.sanitizePath(path);

        // Create a copy of the virtual host to avoid any change into the initial one
        return new VirtualHost(virtualHost.getHost(), sanitizePath, virtualHost.isOverrideEntrypoint());
    }
}

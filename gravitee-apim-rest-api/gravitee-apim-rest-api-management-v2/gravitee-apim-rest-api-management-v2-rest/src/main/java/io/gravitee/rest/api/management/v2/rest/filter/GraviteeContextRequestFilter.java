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
package io.gravitee.rest.api.management.v2.rest.filter;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(10)
public class GraviteeContextRequestFilter implements ContainerRequestFilter {

    private final EnvironmentService environmentService;

    @Inject
    public GraviteeContextRequestFilter(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var principal = (UsernamePasswordAuthenticationToken) requestContext.getSecurityContext().getUserPrincipal();
        var userDetails = Objects.isNull(principal) ? null : (UserDetails) principal.getPrincipal();

        MultivaluedMap<String, String> pathsParams = requestContext.getUriInfo().getPathParameters();

        String organizationId = null;
        if (Objects.nonNull(userDetails)) {
            if (Objects.isNull(userDetails.getOrganizationId())) {
                throw new IllegalStateException("No organization associated to user");
            }
            organizationId = userDetails.getOrganizationId();
        }

        if (pathsParams.containsKey("orgId") && !pathsParams.getFirst("orgId").equals(organizationId)) {
            throw new BadRequestException("Invalid orgId");
        }

        String environmentId = null;
        if (pathsParams.containsKey("envId")) { // The id or hrid of an environment
            String idOrHrid = pathsParams.getFirst("envId");
            environmentId = environmentService.findByOrgAndIdOrHrid(organizationId, idOrHrid).getId();
        }

        GraviteeContext.setCurrentEnvironment(environmentId);
        GraviteeContext.setCurrentOrganization(organizationId);
    }
}

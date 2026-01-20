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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PermissionsResource extends AbstractResource {

    @Inject
    private MembershipService membershipService;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserPermissions(@QueryParam("apiId") String apiId, @QueryParam("applicationId") String applicationId) {
        final String userId = getAuthenticatedUser();
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (apiId != null) {
            if (!accessControlService.canAccessApiFromPortal(executionContext, apiId)) {
                throw new ApiNotFoundException(apiId);
            }
            Map<String, char[]> permissions;
            GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId, false);
            permissions = membershipService.getUserMemberPermissions(executionContext, genericApiEntity, userId);

            return Response.ok(permissions).build();
        } else if (applicationId != null) {
            Set<ApplicationListItem> activeApps = applicationService.findByIdsAndStatus(
                executionContext,
                Collections.singleton(applicationId),
                ApplicationStatus.ACTIVE
            );

            if (activeApps.isEmpty()) {
                throw new ApplicationNotFoundException(applicationId);
            }

            ApplicationEntity application = applicationService.findById(executionContext, applicationId);

            Map<String, char[]> permissions;
            permissions = membershipService.getUserMemberPermissions(executionContext, application, userId);

            return Response.ok(permissions).build();
        }
        throw new BadRequestException("One of the two parameters appId or applicationId must not be null.");
    }

    protected boolean isAdmin() {
        return isUserInRole(ENVIRONMENT_ADMIN);
    }

    private boolean isUserInRole(String role) {
        return securityContext.isUserInRole(role);
    }
}

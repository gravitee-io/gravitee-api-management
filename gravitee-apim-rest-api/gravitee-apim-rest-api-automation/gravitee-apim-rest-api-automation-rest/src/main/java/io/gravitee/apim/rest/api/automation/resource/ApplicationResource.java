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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.ApplicationMapper;
import io.gravitee.apim.rest.api.automation.model.ApplicationSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResource extends AbstractResource {

    @Inject
    private ApplicationService applicationService;

    @PathParam("hrid")
    private String hrid;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = { RolePermissionAction.READ }) })
    public Response getApplicationByHRID(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            ApplicationEntity applicationEntity = applicationService.findById(
                executionContext,
                legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId()
            );
            ApplicationSpec applicationSpec = ApplicationMapper.INSTANCE.applicationEntityToApplicationSpec(applicationEntity);
            return Response
                .ok(
                    ApplicationMapper.INSTANCE.applicationSpecToApplicationState(
                        applicationSpec,
                        applicationEntity.getId(),
                        executionContext.getOrganizationId(),
                        executionContext.getEnvironmentId()
                    )
                )
                .build();
        } catch (ApplicationNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationbyHrid(@QueryParam("legacy") boolean legacy) {
        var executionContext = GraviteeContext.getExecutionContext();

        try {
            applicationService.archive(executionContext, legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId());
        } catch (ApplicationNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }
}

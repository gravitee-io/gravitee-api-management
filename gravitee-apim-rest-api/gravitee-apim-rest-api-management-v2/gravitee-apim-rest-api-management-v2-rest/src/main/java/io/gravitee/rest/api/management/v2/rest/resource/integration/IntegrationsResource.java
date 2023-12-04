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
package io.gravitee.rest.api.management.v2.rest.resource.integration;

import io.gravitee.apim.core.integration.usecase.IntegrationCreateUsecase;
import io.gravitee.apim.core.integration.usecase.IntegrationsGetUsecase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.IntegrationMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateIntegration;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/integrations")
@Slf4j
public class IntegrationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private IntegrationCreateUsecase integrationCreateUsecase;

    @Inject
    private IntegrationsGetUsecase integrationsGetUsecase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INTEGRATION, acls = { RolePermissionAction.CREATE }) })
    public Response createIntegration(@PathParam("envId") String environmentId, @Valid @NotNull final CreateIntegration integration) {
        var newIntegrationEntity = IntegrationMapper.INSTANCE.map(integration);
        newIntegrationEntity.setEnvironmentId(environmentId);

        var createdIntegration = integrationCreateUsecase
            .execute(IntegrationCreateUsecase.Input.builder().integration(newIntegrationEntity).build())
            .createdIntegration();

        return Response
            .created(this.getLocationHeader(createdIntegration.getId()))
            .entity(IntegrationMapper.INSTANCE.map(createdIntegration))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INTEGRATION, acls = { RolePermissionAction.CREATE }) })
    public Response listIntegrations(@PathParam("envId") String environmentId) {
        var integrations = integrationsGetUsecase
            .execute(IntegrationsGetUsecase.Input.builder().environmentId(environmentId).build())
            .integrations();

        return Response.ok().entity(IntegrationMapper.INSTANCE.map(integrations)).build();
    }

    @Path("{integrationId}")
    public IntegrationResource getIntegrationResource() {
        return resourceContext.getResource(IntegrationResource.class);
    }
}

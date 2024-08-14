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

import io.gravitee.apim.core.integration.use_case.CreateIntegrationUseCase;
import io.gravitee.apim.core.integration.use_case.GetIntegrationsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.IntegrationMapper;
import io.gravitee.rest.api.management.v2.rest.model.CreateIntegration;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
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
    private CreateIntegrationUseCase createIntegrationUsecase;

    @Inject
    private GetIntegrationsUseCase getIntegrationsUsecase;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INTEGRATION, acls = { RolePermissionAction.CREATE }) })
    public Response createIntegration(@PathParam("envId") String environmentId, @Valid @NotNull final CreateIntegration integration) {
        var newIntegrationEntity = IntegrationMapper.INSTANCE.map(integration);
        newIntegrationEntity.setEnvironmentId(environmentId);

        var auditInfo = getAuditInfo();

        var createdIntegration = createIntegrationUsecase
            .execute(new CreateIntegrationUseCase.Input(newIntegrationEntity, auditInfo))
            .createdIntegration();

        return Response
            .created(this.getLocationHeader(createdIntegration.getId()))
            .entity(IntegrationMapper.INSTANCE.map(createdIntegration))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INTEGRATION, acls = { RolePermissionAction.READ }) })
    public IntegrationsResponse listIntegrations(
        @PathParam("envId") String environmentId,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        var integrations = getIntegrationsUsecase
            .execute(
                new GetIntegrationsUseCase.Input(
                    GraviteeContext.getCurrentOrganization(),
                    environmentId,
                    new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
                )
            )
            .integrations();
        var totalElements = integrations.getTotalElements();
        return new IntegrationsResponse()
            .data(integrations.getContent().stream().map(IntegrationMapper.INSTANCE::map).toList())
            .pagination(
                PaginationInfo.computePaginationInfo(totalElements, Math.toIntExact(integrations.getPageElements()), paginationParam)
            )
            .links(computePaginationLinks(totalElements, paginationParam));
    }

    @Path("{integrationId}")
    public IntegrationResource getIntegrationResource() {
        return resourceContext.getResource(IntegrationResource.class);
    }
}

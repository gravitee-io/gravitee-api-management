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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.CreateOrUpdateApiDocumentationUseCase;
import io.gravitee.apim.core.portal_page.use_case.ValidateApiDocumentationUseCase;
import io.gravitee.apim.rest.api.automation.mapper.ApiDocumentationMapper;
import io.gravitee.apim.rest.api.automation.model.DocumentationSpec;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
public class ApiDocumentationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateOrUpdateApiDocumentationUseCase createOrUpdateApiDocumentationUseCase;

    @Inject
    private ValidateApiDocumentationUseCase validateApiDocumentationUseCase;

    @Path("/{docHrid}")
    public ApiDocumentationResource getApiDocumentationResource() {
        return resourceContext.getResource(ApiDocumentationResource.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { CREATE, UPDATE }) })
    public Response createOrUpdate(
        @PathParam("apiHrid") String apiHrid,
        @Valid @NotNull DocumentationSpec spec,
        @QueryParam("dryRun") boolean dryRun
    ) {
        var auditInfo = getAuditInfo();
        var apiId = HRIDToUUID.api().context(auditInfo).hrid(apiHrid).id();
        var documentationId = PortalPageContentId.of(
            HRIDToUUID.apiDocumentation().context(auditInfo).api(apiHrid).hrid(spec.getHrid()).id()
        );

        var input = new CreateOrUpdateApiDocumentationUseCase.Input(
            auditInfo,
            documentationId,
            apiId,
            spec.getName(),
            ApiDocumentationMapper.INSTANCE.toDomainType(spec.getType()),
            spec.getContent(),
            spec.getLocation(),
            spec.getOrder()
        );
        var output = dryRun ? validateApiDocumentationUseCase.execute(input) : createOrUpdateApiDocumentationUseCase.execute(input);

        return Response.ok(
            ApiDocumentationMapper.INSTANCE.toDocumentationState(
                spec,
                output.id() != null ? output.id().toString() : null,
                output.errors(),
                auditInfo,
                apiHrid
            )
        ).build();
    }
}

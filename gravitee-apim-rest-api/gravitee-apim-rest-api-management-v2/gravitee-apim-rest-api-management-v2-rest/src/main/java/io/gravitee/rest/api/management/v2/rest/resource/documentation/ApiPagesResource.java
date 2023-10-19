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
package io.gravitee.rest.api.management.v2.rest.resource.documentation;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.usecase.ApiCreateDocumentationPageUsecase;
import io.gravitee.apim.core.documentation.usecase.ApiGetDocumentationPagesUsecase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiDocumentationPagesResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentation;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;

@Path("/environments/{envId}/apis/{apiId}/pages")
public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiGetDocumentationPagesUsecase apiGetDocumentationPagesUsecase;

    @Inject
    private ApiCreateDocumentationPageUsecase apiCreateDocumentationPageUsecase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public Response getApiPages(@PathParam("apiId") String apiId, @QueryParam("parentId") String parentId) {
        final var mapper = Mappers.getMapper(PageMapper.class);
        var result = apiGetDocumentationPagesUsecase.execute(new ApiGetDocumentationPagesUsecase.Input(apiId, parentId));
        var response = ApiDocumentationPagesResponse.builder().pages(mapper.mapPageList(result.pages()));
        if (!StringUtils.isEmpty(parentId)) {
            response.breadcrumb(mapper.map(result.breadcrumbList()));
        }
        return Response.ok(response.build()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.CREATE }) })
    public Response createDocumentationPage(@PathParam("apiId") String apiId, @Valid @NotNull CreateDocumentation createDocumentation) {
        var executionContext = GraviteeContext.getExecutionContext();
        var user = getAuthenticatedUserDetails();
        var auditInfo = AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build())
            .build();

        Page pageToCreate = createDocumentation.getActualInstance() instanceof CreateDocumentationMarkdown
            ? Mappers.getMapper(PageMapper.class).map(createDocumentation.getCreateDocumentationMarkdown())
            : Mappers.getMapper(PageMapper.class).map(createDocumentation.getCreateDocumentationFolder());

        pageToCreate.setReferenceId(apiId);
        pageToCreate.setReferenceType(Page.ReferenceType.API);

        var createdPage = apiCreateDocumentationPageUsecase
            .execute(ApiCreateDocumentationPageUsecase.Input.builder().page(pageToCreate).auditInfo(auditInfo).build())
            .createdPage();

        return Response.ok(Mappers.getMapper(PageMapper.class).mapPage(createdPage)).build();
    }
}

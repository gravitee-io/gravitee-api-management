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
import io.gravitee.apim.core.documentation.use_case.*;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
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
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;

public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiGetDocumentationPagesUseCase apiGetDocumentationPagesUsecase;

    @Inject
    private ApiCreateDocumentationPageUseCase apiCreateDocumentationPageUsecase;

    @Inject
    private ApiGetDocumentationPageUseCase apiGetDocumentationPageUsecase;

    @Inject
    private ApiUpdateDocumentationPageUseCase updateDocumentationPageUsecase;

    @Inject
    private ApiPublishDocumentationPageUseCase apiPublishDocumentationPageUsecase;

    @Inject
    private ApiUnpublishDocumentationPageUseCase apiUnpublishDocumentationPageUsecase;

    @Inject
    private ApiDeleteDocumentationPageUseCase apiDeleteDocumentationPageUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public Response getApiPages(@PathParam("apiId") String apiId, @QueryParam("parentId") String parentId) {
        final var mapper = Mappers.getMapper(PageMapper.class);
        var result = apiGetDocumentationPagesUsecase.execute(new ApiGetDocumentationPagesUseCase.Input(apiId, parentId));
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
        Page pageToCreate = createDocumentation instanceof CreateDocumentationMarkdown
            ? Mappers.getMapper(PageMapper.class).map((CreateDocumentationMarkdown) createDocumentation)
            : Mappers.getMapper(PageMapper.class).map((CreateDocumentationFolder) createDocumentation);

        pageToCreate.setReferenceId(apiId);
        pageToCreate.setReferenceType(Page.ReferenceType.API);

        var createdPage = apiCreateDocumentationPageUsecase
            .execute(ApiCreateDocumentationPageUseCase.Input.builder().page(pageToCreate).auditInfo(getAuditInfo()).build())
            .createdPage();

        return Response
            .created(this.getLocationHeader(createdPage.getId()))
            .entity(Mappers.getMapper(PageMapper.class).mapPage(createdPage))
            .build();
    }

    @GET
    @Path("{pageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public Response getApiPage(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        var page = apiGetDocumentationPageUsecase.execute(new ApiGetDocumentationPageUseCase.Input(apiId, pageId)).page();
        return Response.ok(Mappers.getMapper(PageMapper.class).mapPage(page)).build();
    }

    @PUT
    @Path("{pageId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateDocumentationPage(
        @PathParam("apiId") String apiId,
        @PathParam("pageId") String pageId,
        @Valid @NotNull UpdateDocumentation updateDocumentation
    ) {
        var mapper = Mappers.getMapper(PageMapper.class);
        var auditInfo = getAuditInfo();
        var input = updateDocumentation instanceof UpdateDocumentationMarkdown
            ? mapper.map((UpdateDocumentationMarkdown) updateDocumentation, apiId, pageId, auditInfo)
            : mapper.map((UpdateDocumentationFolder) updateDocumentation, apiId, pageId, auditInfo);

        var page = updateDocumentationPageUsecase.execute(input).page();
        return Response.ok(mapper.mapPage(page)).build();
    }

    @POST
    @Path("{pageId}/_publish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public Response publishDocumentationPage(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        var page = apiPublishDocumentationPageUsecase
            .execute(new ApiPublishDocumentationPageUseCase.Input(apiId, pageId, getAuditInfo()))
            .page();
        return Response.ok(Mappers.getMapper(PageMapper.class).mapPage(page)).build();
    }

    @POST
    @Path("{pageId}/_unpublish")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public Response unpublishDocumentationPage(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        var page = apiUnpublishDocumentationPageUsecase
            .execute(new ApiUnpublishDocumentationPageUseCase.Input(apiId, pageId, getAuditInfo()))
            .page();
        return Response.ok(Mappers.getMapper(PageMapper.class).mapPage(page)).build();
    }

    @DELETE
    @Path("{pageId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.DELETE }) })
    public Response deleteDocumentationPage(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        apiDeleteDocumentationPageUseCase.execute(new ApiDeleteDocumentationPageUseCase.Input(apiId, pageId, getAuditInfo()));
        return Response.noContent().build();
    }
}

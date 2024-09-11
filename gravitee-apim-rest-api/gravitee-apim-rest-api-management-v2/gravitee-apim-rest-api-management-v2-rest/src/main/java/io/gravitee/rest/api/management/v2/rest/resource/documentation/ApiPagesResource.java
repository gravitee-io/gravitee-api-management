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

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.use_case.ApiCreateDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiDeleteDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiGetDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiGetDocumentationPagesUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiPublishDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiUnpublishDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiUpdateDocumentationPageUseCase;
import io.gravitee.apim.core.documentation.use_case.ApiUpdateDocumentationPageUseCase.Input;
import io.gravitee.apim.core.documentation.use_case.ApiUpdateFetchedPageContentUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiDocumentationPagesResponse;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentation;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationAsyncApi;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationFolder;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.model.CreateDocumentationSwagger;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentation;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationAsyncApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationFolder;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationMarkdown;
import io.gravitee.rest.api.management.v2.rest.model.UpdateDocumentationSwagger;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

    @Inject
    private ApiUpdateFetchedPageContentUseCase apiUpdateFetchedPageContentUseCase;

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
        Page pageToCreate = null;
        PageMapper mapper = Mappers.getMapper(PageMapper.class);
        if (createDocumentation instanceof CreateDocumentationMarkdown markdown) {
            pageToCreate = mapper.map(markdown);
        } else if (createDocumentation instanceof CreateDocumentationFolder folder) {
            pageToCreate = mapper.map(folder);
        } else if (createDocumentation instanceof CreateDocumentationSwagger swagger) {
            pageToCreate = mapper.map(swagger);
        } else if (createDocumentation instanceof CreateDocumentationAsyncApi asyncApi) {
            pageToCreate = mapper.map(asyncApi);
        }

        if (pageToCreate != null) {
            pageToCreate.setReferenceId(apiId);
            pageToCreate.setReferenceType(Page.ReferenceType.API);

            Page createdPage = apiCreateDocumentationPageUsecase
                .execute(ApiCreateDocumentationPageUseCase.Input.builder().page(pageToCreate).auditInfo(getAuditInfo()).build())
                .createdPage();

            return Response.created(this.getLocationHeader(createdPage.getId())).entity(mapper.mapPage(createdPage)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
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
        Input input = null;

        if (updateDocumentation instanceof UpdateDocumentationMarkdown markdown) {
            input = mapper.map(markdown, apiId, pageId, auditInfo);
        } else if (updateDocumentation instanceof UpdateDocumentationFolder folder) {
            input = mapper.map(folder, apiId, pageId, auditInfo);
        } else if (updateDocumentation instanceof UpdateDocumentationSwagger swagger) {
            input = mapper.map(swagger, apiId, pageId, auditInfo);
        } else if (updateDocumentation instanceof UpdateDocumentationAsyncApi asyncApi) {
            input = mapper.map(asyncApi, apiId, pageId, auditInfo);
        }

        if (input != null) {
            var page = updateDocumentationPageUsecase.execute(input).page();
            return Response.ok(mapper.mapPage(page)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
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

    @POST
    @Path("{pageId}/_fetch")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.UPDATE }) })
    public Response fetchDocumentationPage(@PathParam("apiId") String apiId, @PathParam("pageId") String pageId) {
        var page = apiUpdateFetchedPageContentUseCase
            .execute(new ApiUpdateFetchedPageContentUseCase.Input(pageId, apiId, getAuditInfo()))
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

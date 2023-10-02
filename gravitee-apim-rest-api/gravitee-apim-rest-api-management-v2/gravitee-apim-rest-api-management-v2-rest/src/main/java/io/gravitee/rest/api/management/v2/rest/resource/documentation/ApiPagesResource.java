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

import io.gravitee.apim.core.documentation.usecase.ApiGetDocumentationPagesUsecase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.mapstruct.factory.Mappers;

@Path("/environments/{envId}/apis/{apiId}/pages")
public class ApiPagesResource extends AbstractResource {

    @Inject
    private ApiGetDocumentationPagesUsecase apiGetDocumentationPagesUsecase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_DOCUMENTATION, acls = { RolePermissionAction.READ }) })
    public Response getApiPages(@PathParam("apiId") String apiId) {
        var result = apiGetDocumentationPagesUsecase.execute(new ApiGetDocumentationPagesUsecase.Input(apiId));
        return Response.ok(Mappers.getMapper(PageMapper.class).mapPageList(result.pages())).build();
    }
}

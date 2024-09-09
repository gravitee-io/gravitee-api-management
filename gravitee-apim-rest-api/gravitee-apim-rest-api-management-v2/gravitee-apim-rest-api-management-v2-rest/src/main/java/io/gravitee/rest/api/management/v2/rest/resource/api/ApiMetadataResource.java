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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.use_case.GetApiMetadataUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.MetadataMapper;
import io.gravitee.rest.api.management.v2.rest.model.MetadataResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

public class ApiMetadataResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private GetApiMetadataUseCase getApiMetadataUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_METADATA, acls = { RolePermissionAction.READ }) })
    public Response getApiMetadata(
        @BeanParam @Valid PaginationParam paginationParam,
        @QueryParam("source") String filterBySource,
        @QueryParam("sortBy") String sortBy
    ) {
        var output = getApiMetadataUseCase.execute(
            new GetApiMetadataUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), filterBySource, sortBy)
        );
        List<ApiMetadata> paginationData = computePaginationData(output.metadata(), paginationParam);

        return Response
            .ok()
            .entity(
                MetadataResponse
                    .builder()
                    .data(MetadataMapper.INSTANCE.mapFromCore(paginationData))
                    .pagination(PaginationInfo.computePaginationInfo(output.metadata().size(), paginationData.size(), paginationParam))
                    .build()
            )
            .build();
    }
}

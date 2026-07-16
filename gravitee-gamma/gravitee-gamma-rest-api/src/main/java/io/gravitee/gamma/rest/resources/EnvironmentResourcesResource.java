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
package io.gravitee.gamma.rest.resources;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.core.domain.resource.use_case.CreateResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.SearchResourceUseCase;
import io.gravitee.gamma.rest.mapper.ResourceMapper;
import io.gravitee.gamma.rest.model.CreateResourceRequest;
import io.gravitee.gamma.rest.model.PaginationInfo;
import io.gravitee.gamma.rest.model.ResourcesResponse;
import io.gravitee.gamma.rest.resources.param.PaginationParam;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/organizations/{orgId}/environments/{envId}/resources")
public class EnvironmentResourcesResource extends AbstractGammaResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateResourceUseCase createResourceUseCase;

    @Inject
    private SearchResourceUseCase searchResourceUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createResource(@Valid @NotNull final CreateResourceRequest request) {
        var output = createResourceUseCase.execute(
            new CreateResourceUseCase.Input(getAuditInfo(), ResourceMapper.INSTANCE.toCommand(request))
        );
        var response = ResourceMapper.INSTANCE.toResponse(output.resource());
        return Response.created(getLocationHeader(output.resource().id())).entity(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcesResponse searchResources(@QueryParam("q") String query, @BeanParam @Valid PaginationParam paginationParam) {
        var output = searchResourceUseCase.execute(new SearchResourceUseCase.Input(getAuditInfo(), paginationParam.toPageable(), query));
        var page = output.resources();
        return new ResourcesResponse(
            ResourceMapper.INSTANCE.toResponseList(page.getContent()),
            PaginationInfo.of(paginationParam.getPage(), paginationParam.getPerPage(), page.getContent().size(), page.getTotalElements())
        );
    }

    @Path("{resourceId}")
    public EnvironmentResourceResource getEnvironmentResourceResource() {
        return resourceContext.getResource(EnvironmentResourceResource.class);
    }
}

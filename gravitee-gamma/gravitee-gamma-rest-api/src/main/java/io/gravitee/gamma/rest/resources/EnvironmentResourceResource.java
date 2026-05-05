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
import io.gravitee.gamma.core.domain.resource.use_case.DeleteResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.GetResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.UpdateResourceUseCase;
import io.gravitee.gamma.rest.mapper.ResourceMapper;
import io.gravitee.gamma.rest.model.ResourceResponse;
import io.gravitee.gamma.rest.model.UpdateResourceRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class EnvironmentResourceResource extends AbstractGammaResource {

    @PathParam("resourceId")
    private String resourceId;

    @Inject
    private GetResourceUseCase getResourceUseCase;

    @Inject
    private UpdateResourceUseCase updateResourceUseCase;

    @Inject
    private DeleteResourceUseCase deleteResourceUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceResponse getResource() {
        var output = getResourceUseCase.execute(new GetResourceUseCase.Input(getAuditInfo(), resourceId));
        return ResourceMapper.INSTANCE.toResponse(output.resource());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResourceResponse updateResource(@Valid @NotNull final UpdateResourceRequest request) {
        var output = updateResourceUseCase.execute(
            new UpdateResourceUseCase.Input(getAuditInfo(), resourceId, ResourceMapper.INSTANCE.toUpdateCommand(request))
        );
        return ResourceMapper.INSTANCE.toResponse(output.resource());
    }

    @DELETE
    public Response deleteResource() {
        deleteResourceUseCase.execute(new DeleteResourceUseCase.Input(getAuditInfo(), resourceId));
        return Response.noContent().build();
    }
}

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

import io.gravitee.apim.core.integration.use_case.IntegrationDeleteUseCase;
import io.gravitee.apim.core.integration.use_case.IntegrationGetUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.IntegrationMapper;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
@Slf4j
public class IntegrationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private IntegrationGetUseCase integrationGetUsecase;

    @Inject
    private IntegrationDeleteUseCase integrationDeleteUsecase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntegrationById(@PathParam("integrationId") String integrationId) {
        var integration = integrationGetUsecase
            .execute(IntegrationGetUseCase.Input.builder().integrationId(integrationId).build())
            .integration();

        return Response.ok(IntegrationMapper.INSTANCE.map(integration)).build();
    }

    @DELETE
    public Response deleteIntegration(@PathParam("integrationId") String integrationId) {
        integrationDeleteUsecase.execute(IntegrationDeleteUseCase.Input.builder().integrationId(integrationId).build());

        return Response.noContent().build();
    }

    @Path("entities")
    public IntegrationEntitiesResource getIntegrationEntitiesResource() {
        return resourceContext.getResource(IntegrationEntitiesResource.class);
    }
}

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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/integrations")
@Slf4j
public class IntegrationsResource extends AbstractResource {

    private static final List<Integration> integrationsMock = new ArrayList<>();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIntegration(@Valid @NotNull final CreateIntegration integration) {
        Integration mockIntegration = Integration
            .builder()
            .id(UUID.randomUUID().toString())
            .name(integration.getName())
            .description(integration.getDescription())
            .provider(integration.getProvider())
            .build();
        integrationsMock.add(mockIntegration);
        return Response.created(this.getLocationHeader(mockIntegration.getId())).entity(mockIntegration).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public IntegrationsResponse listIntegrations(
        @PathParam("envId") String environmentId,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        Page<Integration> integrationPage = new Page<>(integrationsMock, 0, integrationsMock.size(), integrationsMock.size());

        long totalCount = integrationPage.getTotalElements();
        Integer pageItemsCount = Math.toIntExact(integrationPage.getPageElements());

        return new IntegrationsResponse()
            .data(integrationsMock)
            .pagination(PaginationInfo.computePaginationInfo(totalCount, pageItemsCount, paginationParam))
            .links(computePaginationLinks(totalCount, paginationParam));
    }
}

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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.portal_category.use_case.GetVisiblePortalCategoriesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalCategoryMapper;
import io.gravitee.rest.api.portal.rest.model.PortalCategoriesResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Tag(name = "Portal")
public class PortalCategoriesResource extends AbstractResource {

    @Inject
    private GetVisiblePortalCategoriesUseCase getVisiblePortalCategoriesUseCase;

    private final PortalCategoryMapper portalCategoryMapper = PortalCategoryMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalCategories() {
        var executionContext = getExecutionContext();
        var output = getVisiblePortalCategoriesUseCase.execute(
            new GetVisiblePortalCategoriesUseCase.Input(executionContext.getEnvironmentId())
        );

        var responseBody = new PortalCategoriesResponse().data(portalCategoryMapper.map(output.portalCategories()));
        return Response.ok(responseBody).build();
    }
}

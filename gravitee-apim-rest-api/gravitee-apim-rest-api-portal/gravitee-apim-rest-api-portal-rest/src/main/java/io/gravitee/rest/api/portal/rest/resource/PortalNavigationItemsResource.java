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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.use_case.ListPortalNavigationItemsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalNavigationItemMapper;
import io.gravitee.rest.api.portal.rest.model.PortalArea;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Optional;

public class PortalNavigationItemsResource extends AbstractResource {

    @Inject
    private ListPortalNavigationItemsUseCase listPortalNavigationItemsUseCase;

    private final PortalNavigationItemMapper portalNavigationItemMapper = PortalNavigationItemMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalNavigationItems(
        @Nonnull @QueryParam("area") PortalArea area,
        @Nullable @QueryParam("parentId") String parentId,
        @QueryParam("loadChildren") @DefaultValue("true") boolean loadChildren
    ) {
        var executionContext = getExecutionContext();
        var input = new ListPortalNavigationItemsUseCase.Input(
            executionContext.getEnvironmentId(),
            portalNavigationItemMapper.map(area),
            Optional.ofNullable(parentId).map(PortalNavigationItemId::of),
            loadChildren
        );
        var output = listPortalNavigationItemsUseCase.execute(input);
        return Response.ok(portalNavigationItemMapper.map(output.items())).build();
    }
}

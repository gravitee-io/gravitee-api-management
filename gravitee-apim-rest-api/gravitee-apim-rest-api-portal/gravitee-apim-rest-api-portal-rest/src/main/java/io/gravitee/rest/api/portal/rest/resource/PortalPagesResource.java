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

import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PortalPagesResource {

    @Inject
    private GetPortalPageUseCase getPortalPageUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getPortalPages(
        @QueryParam("type") PortalViewContext pageType,
        @QueryParam("expands") List<ExpandsViewContext> expands
    ) {
        final String envId = GraviteeContext.getCurrentEnvironment();

        var output = getPortalPageUseCase.execute(new GetPortalPageUseCase.Input(envId, pageType, expands));
        var pages = output.pages();
        List<PortalPageWithViewDetails> filteredPages = Optional.ofNullable(pages)
            .orElse(Collections.emptyList())
            .stream()
            .filter(page -> page.viewDetails().published())
            .toList();
        return Response.ok(filteredPages).build();
    }
}

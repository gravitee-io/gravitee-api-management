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

import io.gravitee.apim.core.agent.use_case.GetPortalAgentUseCase;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.AgentMapper;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class AgentsResource extends AbstractResource {

    private static final AgentMapper agentMapper = AgentMapper.INSTANCE;

    @Inject
    private GetPortalAgentUseCase getPortalAgentUseCase;

    @GET
    @Path("{agentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getAgentByAgentId(@PathParam("agentId") String agentId) {
        var executionContext = getExecutionContext();
        var output = getPortalAgentUseCase.execute(
            new GetPortalAgentUseCase.Input(
                executionContext.getEnvironmentId(),
                agentId,
                PortalNavigationItemViewerContext.forPortal(getAuthenticatedUserOrNull())
            )
        );

        return Response.ok(agentMapper.map(output.agent())).build();
    }
}

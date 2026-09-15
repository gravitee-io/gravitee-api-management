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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.agent;

import io.gravitee.apim.core.analytics.use_case.GetAgentActivityUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Agent activity feed — lists this agent's runs, grouped by conversation-id when present,
 * with decisions joined by request-id.
 *
 * <p>Called by the AIM module's Activity page. Not a replacement for Sam's Approvals / Decisions.
 *
 * <p>This is a sub-resource delegated from {@code EnvironmentAnalyticsResource}
 * at {@code /analytics/agent-activity}. No class-level {@code @Path} — path is inherited.
 */
public class AgentActivityResource extends AbstractResource {

    @Inject
    GetAgentActivityUseCase getAgentActivityUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgentActivity(
        @QueryParam("a2aApiId") String a2aApiId,
        @QueryParam("applicationIds") List<String> applicationIds,
        @QueryParam("actorId") String actorId,
        @QueryParam("from") @DefaultValue("0") Long from,
        @QueryParam("to") @DefaultValue("0") Long to,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("25") int size
    ) {
        if (!canReadDashboards()) {
            throw new ForbiddenAccessException();
        }

        var executionContext = GraviteeContext.getExecutionContext();
        var input = new GetAgentActivityUseCase.Input(a2aApiId, applicationIds, actorId, from, to, page, size);
        var result = getAgentActivityUseCase.execute(executionContext, input);
        return Response.ok(result).build();
    }
}

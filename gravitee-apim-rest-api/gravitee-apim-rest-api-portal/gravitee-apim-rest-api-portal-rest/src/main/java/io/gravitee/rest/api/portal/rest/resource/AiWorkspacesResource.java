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

import io.gravitee.apim.core.ai_workspace.exception.AiWorkspaceNotFoundException;
import io.gravitee.apim.core.ai_workspace.use_case.GetMyAiWorkspaceConsumptionUseCase;
import io.gravitee.apim.core.ai_workspace.use_case.GetMyAiWorkspaceUseCase;
import io.gravitee.apim.core.ai_workspace.use_case.ListMyAiWorkspacesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.mapper.AiWorkspaceMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

public class AiWorkspacesResource extends AbstractResource {

    @Inject
    private ListMyAiWorkspacesUseCase listMyAiWorkspacesUseCase;

    @Inject
    private GetMyAiWorkspaceUseCase getMyAiWorkspaceUseCase;

    @Inject
    private GetMyAiWorkspaceConsumptionUseCase getMyAiWorkspaceConsumptionUseCase;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ParameterService parameterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@BeanParam PaginationParam pagination, @QueryParam("name") String name) {
        requireEnabled();
        var executionContext = getExecutionContext();
        var page = listMyAiWorkspacesUseCase
            .execute(
                new ListMyAiWorkspacesUseCase.Input(executionContext, applicationIds(), name, pagination.getPage(), pagination.getSize())
            )
            .page();
        return Response.ok(AiWorkspaceMapper.toResponse(page)).build();
    }

    @GET
    @Path("{aiWorkspaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("aiWorkspaceId") String aiWorkspaceId) {
        requireEnabled();
        var details = getMyAiWorkspaceUseCase
            .execute(new GetMyAiWorkspaceUseCase.Input(getExecutionContext(), applicationIds(), aiWorkspaceId))
            .details();
        return Response.ok(AiWorkspaceMapper.toDetails(details)).build();
    }

    @GET
    @Path("{aiWorkspaceId}/consumption")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumption(@PathParam("aiWorkspaceId") String aiWorkspaceId) {
        requireEnabled();
        var consumption = getMyAiWorkspaceConsumptionUseCase
            .execute(new GetMyAiWorkspaceConsumptionUseCase.Input(getExecutionContext(), applicationIds(), aiWorkspaceId))
            .consumption();
        return Response.ok(AiWorkspaceMapper.toConsumption(consumption)).build();
    }

    private void requireEnabled() {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        if (
            !parameterService.findAsBoolean(
                getExecutionContext(),
                Key.PORTAL_NEXT_AI_WORKSPACES_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ) {
            throw new AiWorkspaceNotFoundException("ai-workspaces");
        }
    }

    private Set<String> applicationIds() {
        var applications = applicationService.findByUser(getExecutionContext(), getAuthenticatedUser());
        if (applications == null || applications.isEmpty()) {
            return Set.of();
        }
        return applications
            .stream()
            .map(ApplicationListItem::getId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());
    }
}

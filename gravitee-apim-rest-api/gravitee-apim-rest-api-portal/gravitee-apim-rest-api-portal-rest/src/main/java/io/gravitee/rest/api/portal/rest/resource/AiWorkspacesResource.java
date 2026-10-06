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

import io.gravitee.apim.core.ai_workspace.use_case.GetMyAiWorkspaceUseCase;
import io.gravitee.apim.core.ai_workspace.use_case.ListMyAiWorkspacesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.portal.rest.mapper.AiWorkspaceMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ApplicationService;
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
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@BeanParam PaginationParam pagination, @QueryParam("name") String name) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        var executionContext = getExecutionContext();
        var workspaces = listMyAiWorkspacesUseCase
            .execute(new ListMyAiWorkspacesUseCase.Input(executionContext, applicationIds(), name))
            .workspaces()
            .stream()
            .map(AiWorkspaceMapper.INSTANCE::toSummary)
            .toList();
        return createListResponse(executionContext, workspaces, pagination);
    }

    @GET
    @Path("{aiWorkspaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("aiWorkspaceId") String aiWorkspaceId) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        var details = getMyAiWorkspaceUseCase
            .execute(new GetMyAiWorkspaceUseCase.Input(getExecutionContext(), applicationIds(), aiWorkspaceId))
            .details();
        return Response.ok(AiWorkspaceMapper.INSTANCE.toDetails(details)).build();
    }

    private Set<String> applicationIds() {
        var applications = applicationService.findByUser(getExecutionContext(), getAuthenticatedUser());
        if (applications == null || applications.isEmpty()) {
            return Set.of();
        }
        String callerId = getAuthenticatedUser();
        return applications
            .stream()
            .filter(application -> ownedByCaller(application, callerId))
            .map(ApplicationListItem::getId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());
    }

    private static boolean ownedByCaller(ApplicationListItem application, String callerId) {
        PrimaryOwnerEntity owner = application.getPrimaryOwner();
        return owner != null && "USER".equals(owner.getType()) && callerId.equals(owner.getId());
    }
}

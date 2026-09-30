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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.stream.Collectors;

public class AiWorkspacesResource extends AbstractResource {

    @Inject
    private ListMyAiWorkspacesUseCase listMyAiWorkspacesUseCase;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@BeanParam PaginationParam pagination) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        var executionContext = getExecutionContext();
        var workspaces = listMyAiWorkspacesUseCase
            .execute(new ListMyAiWorkspacesUseCase.Input(executionContext, applicationIds()))
            .workspaces()
            .stream()
            .map(AiWorkspaceMapper.INSTANCE::toSummary)
            .toList();
        return createListResponse(executionContext, workspaces, pagination);
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

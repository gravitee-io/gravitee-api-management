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

import io.gravitee.apim.core.invitation.use_case.SearchApplicationInvitationsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationInvitationMapper;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationInvitationsSearchCriteriaMapper;
import io.gravitee.rest.api.portal.rest.model.ApplicationInvitationsSearchInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Application")
public class ApplicationInvitationsResource extends AbstractResource {

    private static final ApplicationInvitationMapper INVITATION_MAPPER = ApplicationInvitationMapper.INSTANCE;

    @Inject
    private SearchApplicationInvitationsUseCase searchApplicationInvitationsUseCase;

    @POST
    @Path("/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public Response searchInvitationsByApplicationId(
        @PathParam("applicationId") String applicationId,
        @Valid @BeanParam PaginationParam paginationParam,
        @Valid @NotNull(message = "Input must not be null.") ApplicationInvitationsSearchInput input
    ) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = searchApplicationInvitationsUseCase.execute(
            new SearchApplicationInvitationsUseCase.Input(
                executionContext,
                applicationId,
                ApplicationInvitationsSearchCriteriaMapper.INSTANCE.toSearchCriteria(input),
                new PageableImpl(paginationParam.getPage(), paginationParam.getSize())
            )
        );
        var invitations = INVITATION_MAPPER.toInvitation(output.invitations().getContent());
        Map<String, Map<String, Object>> metadata = new HashMap<>(
            Map.of("paginateMetaData", new HashMap<>(Map.of("totalElements", output.invitations().getTotalElements())))
        );

        return createListResponse(executionContext, invitations, paginationParam, metadata);
    }
}

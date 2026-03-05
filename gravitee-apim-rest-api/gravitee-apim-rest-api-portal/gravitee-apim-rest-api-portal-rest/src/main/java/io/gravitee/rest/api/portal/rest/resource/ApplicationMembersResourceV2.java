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

import io.gravitee.apim.core.application_member.use_case.GetApplicationMembersUseCase;
import io.gravitee.apim.core.application_member.use_case.UpdateApplicationMemberUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.MemberV2Mapper;
import io.gravitee.rest.api.portal.rest.model.MemberV2Input;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class ApplicationMembersResourceV2 extends AbstractResource {

    @Inject
    private GetApplicationMembersUseCase getApplicationMembersUseCase;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UpdateApplicationMemberUseCase updateApplicationMemberUseCase;

    private static final MemberV2Mapper MEMBER_V2_MAPPER = MemberV2Mapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public Response getMembersByApplicationIdV2(
        @PathParam("applicationId") String applicationId,
        @QueryParam("q") String query,
        @BeanParam PaginationParam paginationParam
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        var result = getApplicationMembersUseCase.execute(
            new GetApplicationMembersUseCase.Input(
                applicationId,
                executionContext.getEnvironmentId(),
                query,
                paginationParam.getPage(),
                paginationParam.getSize()
            )
        );

        Map<String, Object> totalOnly = new HashMap<>();
        totalOnly.put("totalElements", result.totalElements());
        Map<String, Map<String, Object>> metadata = new HashMap<>();
        metadata.put("paginateMetaData", totalOnly);

        var mappedMembers = result.members().stream().map(MEMBER_V2_MAPPER::map).toList();
        return createListResponse(executionContext, mappedMembers, paginationParam, metadata);
    }

    @PUT
    @Path("{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationMemberByApplicationIdAndMemberIdV2(
        @PathParam("applicationId") String applicationId,
        @PathParam("memberId") String memberId,
        @Valid @NotNull(message = "Input must not be null.") MemberV2Input memberInput
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        if (memberInput.getUser() != null && !memberId.equals(memberInput.getUser())) {
            throw new BadRequestException("'memberInput.user' should the same as 'memberId'");
        }

        var result = updateApplicationMemberUseCase.execute(
            new UpdateApplicationMemberUseCase.Input(applicationId, memberId, memberInput.getRole(), executionContext.getOrganizationId())
        );

        return Response.ok(MEMBER_V2_MAPPER.map(result.updatedMember())).build();
    }
}

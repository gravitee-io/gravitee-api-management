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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.application_member.use_case.AddApplicationMemberUseCase;
import io.gravitee.apim.core.application_member.use_case.DeleteApplicationMemberUseCase;
import io.gravitee.apim.core.application_member.use_case.GetApplicationMembersUseCase;
import io.gravitee.apim.core.application_member.use_case.SearchUsersForApplicationMemberUseCase;
import io.gravitee.apim.core.application_member.use_case.TransferApplicationOwnershipUseCase;
import io.gravitee.apim.core.application_member.use_case.UpdateApplicationMemberUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.MemberV2Mapper;
import io.gravitee.rest.api.portal.rest.model.MemberV2Input;
import io.gravitee.rest.api.portal.rest.model.SearchUserV2;
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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationMembersResourceV2 extends AbstractResource {

    @Inject
    private GetApplicationMembersUseCase getApplicationMembersUseCase;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UpdateApplicationMemberUseCase updateApplicationMemberUseCase;

    @Inject
    private DeleteApplicationMemberUseCase deleteApplicationMemberUseCase;

    @Inject
    private SearchUsersForApplicationMemberUseCase searchUsersForApplicationMemberUseCase;

    @Inject
    private AddApplicationMemberUseCase addApplicationMemberUseCase;

    @Inject
    private TransferApplicationOwnershipUseCase transferApplicationOwnershipUseCase;

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response createApplicationMemberV2(
        @PathParam("applicationId") String applicationId,
        @QueryParam("notify") @DefaultValue("true") boolean queryNotify,
        @Valid @NotNull(message = "Input must not be null.") JsonNode addMemberInput
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        var parsedPayload = parseAddMembersInput(addMemberInput, queryNotify);
        var result = addApplicationMemberUseCase.execute(
            new AddApplicationMemberUseCase.Input(
                applicationId,
                parsedPayload.members(),
                parsedPayload.sendNotification(),
                executionContext.getEnvironmentId(),
                executionContext.getOrganizationId()
            )
        );

        if (parsedPayload.batchRequest()) {
            var createdMembers = result.createdMembers().stream().map(MEMBER_V2_MAPPER::map).toList();
            return Response.status(Response.Status.CREATED).entity(createdMembers).build();
        }

        var createdMember = result
            .createdMembers()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No member created."));
        var responseBuilder = createdMember.getId() == null
            ? Response.status(Response.Status.CREATED)
            : Response.created(this.getLocationHeader(createdMember.getId()));
        return responseBuilder.entity(MEMBER_V2_MAPPER.map(createdMember)).build();
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

    @DELETE
    @Path("{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMemberV2(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        deleteApplicationMemberUseCase.execute(new DeleteApplicationMemberUseCase.Input(applicationId, memberId));

        return Response.noContent().build();
    }

    @POST
    @Path("_search-users")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response searchUsersForApplicationMembersV2(
        @PathParam("applicationId") String applicationId,
        @QueryParam("q") String query,
        @BeanParam PaginationParam paginationParam
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        var result = searchUsersForApplicationMemberUseCase.execute(
            new SearchUsersForApplicationMemberUseCase.Input(applicationId, query, executionContext.getEnvironmentId())
        );

        var mappedUsers = result
            .users()
            .stream()
            .map(user ->
                new SearchUserV2()
                    .id(user.id())
                    .reference(user.reference())
                    .firstName(user.firstName())
                    .lastName(user.lastName())
                    .displayName(user.displayName())
                    .email(user.email())
            );
        return createListResponse(executionContext, mappedUsers.toList(), paginationParam);
    }

    @POST
    @Path("_transfer-ownership")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferApplicationOwnershipV2(
        @PathParam("applicationId") String applicationId,
        @Valid @NotNull(message = "Input must not be null.") JsonNode transferOwnershipInput
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        var parsedInput = parseTransferOwnershipInput(transferOwnershipInput);
        transferApplicationOwnershipUseCase.execute(
            new TransferApplicationOwnershipUseCase.Input(
                applicationId,
                parsedInput.newPrimaryOwnerId(),
                parsedInput.newPrimaryOwnerReference(),
                parsedInput.previousOwnerNewRole(),
                executionContext.getOrganizationId()
            )
        );
        return Response.noContent().build();
    }

    private ParsedAddMembersInput parseAddMembersInput(JsonNode addMemberInput, boolean queryNotify) {
        if (addMemberInput.has("members") && addMemberInput.get("members").isArray()) {
            var members = new ArrayList<AddApplicationMemberUseCase.AddMemberRequest>();
            addMemberInput
                .get("members")
                .forEach(member ->
                    members.add(
                        new AddApplicationMemberUseCase.AddMemberRequest(
                            nullableText(member, "userId"),
                            nullableText(member, "reference"),
                            nullableText(member, "role")
                        )
                    )
                );

            var notify = addMemberInput.has("notify") && addMemberInput.get("notify").isBoolean()
                ? addMemberInput.get("notify").asBoolean()
                : queryNotify;
            return new ParsedAddMembersInput(members, notify, true);
        }

        var memberInput = new MemberV2Input()
            .user(nullableText(addMemberInput, "user"))
            .reference(nullableText(addMemberInput, "reference"))
            .role(nullableText(addMemberInput, "role"));
        return new ParsedAddMembersInput(
            List.of(
                new AddApplicationMemberUseCase.AddMemberRequest(memberInput.getUser(), memberInput.getReference(), memberInput.getRole())
            ),
            queryNotify,
            false
        );
    }

    private String nullableText(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private ParsedTransferOwnershipInput parseTransferOwnershipInput(JsonNode transferOwnershipInput) {
        return new ParsedTransferOwnershipInput(
            firstNonBlank(
                nullableText(transferOwnershipInput, "newOwnerId"),
                nullableText(transferOwnershipInput, "newPrimaryOwnerId"),
                nullableText(transferOwnershipInput, "new_primary_owner_id")
            ),
            firstNonBlank(
                nullableText(transferOwnershipInput, "newOwnerReference"),
                nullableText(transferOwnershipInput, "newPrimaryOwnerReference"),
                nullableText(transferOwnershipInput, "new_primary_owner_reference")
            ),
            firstNonBlank(
                nullableText(transferOwnershipInput, "previousOwnerNewRole"),
                nullableText(transferOwnershipInput, "primaryOwnerNewrole"),
                nullableText(transferOwnershipInput, "primary_owner_newrole")
            )
        );
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private record ParsedAddMembersInput(
        List<AddApplicationMemberUseCase.AddMemberRequest> members,
        boolean sendNotification,
        boolean batchRequest
    ) {}

    private record ParsedTransferOwnershipInput(String newPrimaryOwnerId, String newPrimaryOwnerReference, String previousOwnerNewRole) {}
}

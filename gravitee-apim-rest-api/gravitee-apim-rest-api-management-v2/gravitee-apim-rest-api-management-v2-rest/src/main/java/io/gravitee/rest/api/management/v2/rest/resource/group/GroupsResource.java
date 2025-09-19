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
package io.gravitee.rest.api.management.v2.rest.resource.group;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.use_case.ImportGroupCRDUseCase;
import io.gravitee.apim.core.group.use_case.SearchGroupsUseCase;
import io.gravitee.apim.core.group.use_case.ValidateGroupCRDUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.mapper.GroupCRDMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.GroupMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.GroupCRDSpec;
import io.gravitee.rest.api.management.v2.rest.model.GroupSearchParams;
import io.gravitee.rest.api.management.v2.rest.model.GroupsResponse;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.IntegrationPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/environments/{envId}/groups")
public class GroupsResource extends AbstractResource {

    @Inject
    private GroupService groupService;

    @Inject
    private ValidateGroupCRDUseCase validateGroupCRD;

    @Inject
    private ImportGroupCRDUseCase importGroupCRD;

    private static final GroupMapper MAPPER = GroupMapper.INSTANCE;

    @Inject
    private SearchGroupsUseCase searchGroupsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { RolePermissionAction.READ }) })
    public GroupsResponse listGroups(@BeanParam @Valid PaginationParam paginationParam) {
        List<GroupEntity> groups = groupService.findAll(GraviteeContext.getExecutionContext());

        List<GroupEntity> groupsSubset = computePaginationData(groups, paginationParam);

        return new GroupsResponse()
            .data(MAPPER.map(groupsSubset))
            .pagination(PaginationInfo.computePaginationInfo(groups.size(), groupsSubset.size(), paginationParam))
            .links(computePaginationLinks(groups.size(), paginationParam));
    }

    @POST
    @Path("/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { RolePermissionAction.READ }) })
    public GroupsResponse searchGroups(@BeanParam PaginationParam paginationParam, @Valid GroupSearchParams searchParams) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        Page<Group> pagedGroups = searchGroupsUseCase.execute(executionContext, searchParams.getIds(), paginationParam.toPageable());

        io.gravitee.rest.api.management.v2.rest.model.Links links = computePaginationLinks(pagedGroups.getTotalElements(), paginationParam);

        return new GroupsResponse()
            .data(MAPPER.mapFromCoreList(pagedGroups.getContent()))
            .pagination(PaginationInfo.computePaginationInfo(pagedGroups.getPageElements(), paginationParam.getPerPage(), paginationParam))
            .links(links);
    }

    @GET
    @Path("/{groupId}/members")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { RolePermissionAction.READ }),
        }
    )
    public MembersResponse listGroupMembers(@PathParam("groupId") String groupId, @BeanParam @Valid PaginationParam paginationParam) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        GroupEntity groupEntity = groupService.findById(executionContext, groupId);

        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("groupName", groupEntity.getName());

        var members = membershipService
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, groupId)
            .stream()
            .map(MemberMapper.INSTANCE::map)
            .sorted(Comparator.comparing(Member::getId))
            .collect(Collectors.toList());

        List<Member> membersSubset = computePaginationData(members, paginationParam);

        return new MembersResponse()
            .data(membersSubset)
            .pagination(PaginationInfo.computePaginationInfo(members.size(), membersSubset.size(), paginationParam))
            .links(computePaginationLinks(members.size(), paginationParam))
            .metadata(metadata);
    }

    @GET
    @Path("/{groupId}/permissions")
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @ApiResponse(
        responseCode = "200",
        description = "Integration permissions",
        content = @Content(
            mediaType = io.gravitee.common.http.MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MemberEntity.class))
        )
    )
    public Map<String, char[]> getPermissions(@PathParam("groupId") String groupId) {
        if (isAdmin()) {
            final char[] rights = new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), RolePermissionAction.DELETE.getId() };
            return Arrays.stream(IntegrationPermission.values()).collect(
                Collectors.toMap(IntegrationPermission::getName, ignored -> rights)
            );
        } else if (isAuthenticated()) {
            final String username = getAuthenticatedUser();
            final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            return membershipService.getUserMemberPermissions(executionContext, MembershipReferenceType.GROUP, groupId, username);
        } else {
            return Map.of();
        }
    }

    @PUT
    @Path("/_import/crd")
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.CREATE) })
    public Response importGroupCRD(@Valid GroupCRDSpec spec, @QueryParam("dryRun") boolean dryRun) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var input = new ImportGroupCRDUseCase.Input(
            AuditInfo.builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .actor(
                    AuditActor.builder()
                        .userId(userDetails.getUsername())
                        .userSource(userDetails.getSource())
                        .userSourceId(userDetails.getSourceId())
                        .build()
                )
                .build(),
            GroupCRDMapper.INSTANCE.toCore(spec)
        );

        var output = dryRun ? validateGroupCRD.execute(input) : importGroupCRD.execute(input);

        return Response.ok(output.status()).build();
    }
}

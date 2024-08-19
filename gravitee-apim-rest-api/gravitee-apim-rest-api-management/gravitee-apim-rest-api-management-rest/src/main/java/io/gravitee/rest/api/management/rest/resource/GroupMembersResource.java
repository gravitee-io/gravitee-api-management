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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException.Type.SYSTEM;
import static io.gravitee.rest.api.service.notification.PortalHook.GROUP_INVITATION;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.GroupMembership;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupInvitationForbiddenException;
import io.gravitee.rest.api.service.exceptions.GroupMembersLimitationExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Group Memberships")
public class GroupMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @Inject
    private UserService userService;

    @Inject
    private NotifierService notifierService;

    @Inject
    private EventManager eventManager;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("group")
    @Parameter(name = "group", hidden = true)
    private String group;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List group members")
    @ApiResponse(
        responseCode = "200",
        description = "List of group's members",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = GroupMemberEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.READ),
        }
    )
    public List<GroupMemberEntity> getGroupMembers() {
        return new ArrayList<>(getGroupMembers(null).getData());
    }

    @GET
    @Path("_paged")
    @Produces(io.gravitee.common.http.MediaType.APPLICATION_JSON)
    @Operation(summary = "List group members with pagination")
    @ApiResponse(
        responseCode = "200",
        description = "List of group's members",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = GroupMemberEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.READ),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.READ),
        }
    )
    public PagedResult<GroupMemberEntity> getGroupMembers(@Valid @BeanParam Pageable pageable) {
        //check that group exists
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        groupService.findById(executionContext, group);

        io.gravitee.rest.api.model.common.Pageable commonPageable = null;

        if (pageable != null) {
            commonPageable = pageable.toPageable();
        }

        Page<MemberEntity> membersPage = membershipService.getMembersByReference(
            executionContext,
            MembershipReferenceType.GROUP,
            group,
            commonPageable
        );

        Map<String, List<MemberEntity>> members = membersPage
            .getContent()
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(MemberEntity::getId));

        List<GroupMemberEntity> groupMemberEntities = members
            .keySet()
            .stream()
            .map(id -> new GroupMemberEntity(members.get(id).get(0)))
            .sorted(Comparator.comparing(GroupMemberEntity::getId))
            .collect(toList());

        return new PagedResult<>(
            groupMemberEntities,
            membersPage.getPageNumber(),
            (int) membersPage.getPageElements(),
            (int) membersPage.getTotalElements()
        );
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add or update a group member")
    @ApiResponse(responseCode = "201", description = "Member has been added")
    @ApiResponse(responseCode = "200", description = "Member has been updated")
    @ApiResponse(responseCode = "400", description = "Membership is not valid")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(
        {
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.CREATE),
            @Permission(value = ENVIRONMENT_GROUP, acls = RolePermissionAction.UPDATE),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.CREATE),
            @Permission(value = RolePermission.GROUP_MEMBER, acls = RolePermissionAction.UPDATE),
        }
    )
    public Response addOrUpdateGroupMember(@Valid @NotNull final List<GroupMembership> memberships) {
        // Check that group exists
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final GroupEntity groupEntity = groupService.findById(executionContext, group);

        // check if user is a 'simple group admin' or a platform admin
        final boolean hasPermission = permissionService.hasPermission(
            executionContext,
            ENVIRONMENT_GROUP,
            GraviteeContext.getCurrentEnvironment(),
            CREATE,
            UPDATE,
            DELETE
        );
        if (!hasPermission) {
            if (groupEntity.getMaxInvitation() != null) {
                final Set<MemberEntity> members = membershipService.getMembersByReference(
                    executionContext,
                    MembershipReferenceType.GROUP,
                    group
                );
                final long membershipsToAddSize = memberships
                    .stream()
                    .map(GroupMembership::getId)
                    .filter(s -> {
                        final List<String> membershipIdsToSave = members.stream().map(MemberEntity::getId).toList();
                        return !membershipIdsToSave.contains(s);
                    })
                    .count();
                if ((groupService.getNumberOfMembers(executionContext, group) + membershipsToAddSize) > groupEntity.getMaxInvitation()) {
                    throw new GroupMembersLimitationExceededException(groupEntity.getMaxInvitation());
                }
            }
            if (!groupEntity.isSystemInvitation()) {
                throw new GroupInvitationForbiddenException(SYSTEM, group);
            }
        }

        for (GroupMembership membership : memberships) {
            RoleEntity previousApiRole = null;
            RoleEntity previousApplicationRole = null;
            RoleEntity previousGroupRole = null;
            RoleEntity previousIntegrationRole = null;

            if (membership.getId() != null) {
                Set<RoleEntity> userRoles = membershipService.getRoles(
                    MembershipReferenceType.GROUP,
                    group,
                    MembershipMemberType.USER,
                    membership.getId()
                );
                for (RoleEntity role : userRoles) {
                    switch (role.getScope()) {
                        case API:
                            previousApiRole = role;
                            break;
                        case APPLICATION:
                            previousApplicationRole = role;
                            break;
                        case GROUP:
                            previousGroupRole = role;
                            break;
                        case INTEGRATION:
                            previousIntegrationRole = role;
                            break;
                        default:
                            break;
                    }
                }
            }

            // Process add / update before delete to avoid having a user without role
            if (membership.getRoles() != null && !membership.getRoles().isEmpty()) {
                Map<RoleScope, RoleEntity> roleEntities = new HashMap<>();
                for (MemberRoleEntity item : membership.getRoles()) {
                    roleService
                        .findByScopeAndName(item.getRoleScope(), item.getRoleName(), GraviteeContext.getCurrentOrganization())
                        .ifPresent(roleEntity -> roleEntities.put(item.getRoleScope(), roleEntity));
                }

                MemberEntity updatedMembership = null;

                // Replace if new role to add
                RoleEntity apiRoleEntity = roleEntities.get(RoleScope.API);
                if (apiRoleEntity != null && !apiRoleEntity.equals(previousApiRole)) {
                    String roleName = getRoleName(RoleScope.API, apiRoleEntity, groupEntity, GroupEntity::isLockApiRole, hasPermission);
                    updatedMembership = updateRole(RoleScope.API, roleName, previousApiRole, membership, executionContext);
                    if (previousApiRole != null && previousApiRole.getName().equals(SystemRole.PRIMARY_OWNER.name())) {
                        groupService.updateApiPrimaryOwner(group, null);
                    } else if (roleName.equals(SystemRole.PRIMARY_OWNER.name())) {
                        groupService.updateApiPrimaryOwner(group, updatedMembership.getId());
                    }
                }

                RoleEntity applicationRoleEntity = roleEntities.get(RoleScope.APPLICATION);
                if (applicationRoleEntity != null && !applicationRoleEntity.equals(previousApplicationRole)) {
                    String roleName = getRoleName(
                        RoleScope.APPLICATION,
                        applicationRoleEntity,
                        groupEntity,
                        GroupEntity::isLockApplicationRole,
                        hasPermission
                    );
                    updateRole(RoleScope.APPLICATION, roleName, previousApplicationRole, membership, executionContext);
                }

                RoleEntity integrationRoleEntity = roleEntities.get(RoleScope.INTEGRATION);
                if (integrationRoleEntity != null && !integrationRoleEntity.equals(previousIntegrationRole)) {
                    String roleName = getRoleName(RoleScope.INTEGRATION, integrationRoleEntity, groupEntity, e -> true, hasPermission);
                    updateRole(RoleScope.INTEGRATION, roleName, previousIntegrationRole, membership, executionContext);
                }

                RoleEntity groupRoleEntity = roleEntities.get(RoleScope.GROUP);
                if (groupRoleEntity != null && !groupRoleEntity.equals(previousGroupRole)) {
                    updateRole(RoleScope.GROUP, groupRoleEntity.getName(), previousGroupRole, membership, executionContext);
                }

                // Delete if existing and new role is empty
                var membershipId = membership.getId();
                deleteIfNewAndPreviousRoleNull(apiRoleEntity, previousApiRole, membershipId);
                deleteIfNewAndPreviousRoleNull(applicationRoleEntity, previousApplicationRole, membershipId);
                deleteIfNewAndPreviousRoleNull(integrationRoleEntity, previousIntegrationRole, membershipId);
                deleteIfNewAndPreviousRoleNull(groupRoleEntity, previousGroupRole, membershipId);

                // Send notification
                if (
                    previousApiRole == null &&
                    previousApplicationRole == null &&
                    previousGroupRole == null &&
                    previousIntegrationRole == null &&
                    updatedMembership != null
                ) {
                    UserEntity userEntity = this.userService.findById(executionContext, updatedMembership.getId());
                    Map<String, Object> params = new HashMap<>();
                    params.put("group", groupEntity);
                    params.put("user", userEntity);
                    this.notifierService.trigger(executionContext, GROUP_INVITATION, params);
                }
            }
        }

        eventManager.publishEvent(
            ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE,
            new ApplicationAlertMembershipEvent(executionContext.getOrganizationId(), Collections.emptySet(), Collections.singleton(group))
        );

        return Response.ok().build();
    }

    private void deleteIfNewAndPreviousRoleNull(RoleEntity newRole, RoleEntity previousRole, String membershipId) {
        if (newRole == null && previousRole != null) {
            membershipService.removeRole(
                MembershipReferenceType.GROUP,
                group,
                MembershipMemberType.USER,
                membershipId,
                previousRole.getId()
            );
        }
    }

    String getRoleName(
        RoleScope scope,
        RoleEntity roleEntity,
        GroupEntity groupEntity,
        Predicate<GroupEntity> isLocked,
        boolean hasPermission
    ) {
        String roleName = roleEntity.getName();
        if (!hasPermission && isLocked.test(groupEntity)) {
            if (groupEntity.getRoles() != null && !groupEntity.getRoles().isEmpty()) {
                roleName = groupEntity.getRoles().get(scope);
            } else {
                final List<RoleEntity> defaultRoles = roleService.findDefaultRoleByScopes(GraviteeContext.getCurrentOrganization(), scope);
                if (defaultRoles != null && !defaultRoles.isEmpty()) {
                    roleName = defaultRoles.get(0).getName();
                }
            }
        }
        return roleName;
    }

    MemberEntity updateRole(
        RoleScope scope,
        String newRoleName,
        RoleEntity previousRole,
        GroupMembership membership,
        ExecutionContext executionContext
    ) {
        var updatedMembership = membershipService.addRoleToMemberOnReference(
            executionContext,
            new MembershipService.MembershipReference(MembershipReferenceType.GROUP, group),
            new MembershipService.MembershipMember(membership.getId(), membership.getReference(), MembershipMemberType.USER),
            new MembershipService.MembershipRole(scope, newRoleName)
        );
        if (previousRole != null) {
            membershipService.removeRole(
                MembershipReferenceType.GROUP,
                group,
                MembershipMemberType.USER,
                updatedMembership.getId(),
                previousRole.getId()
            );
        }
        return updatedMembership;
    }

    @Path("{member}")
    public GroupMemberResource groupMemberResource() {
        return resourceContext.getResource(GroupMemberResource.class);
    }
}

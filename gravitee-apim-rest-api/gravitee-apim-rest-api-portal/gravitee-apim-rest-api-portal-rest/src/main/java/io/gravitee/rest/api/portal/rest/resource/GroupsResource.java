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

import static java.util.stream.Collectors.toList;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.model.Group;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GroupService groupService;

    @Inject
    private MemberMapper memberMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public Response findAll(@BeanParam PaginationParam paginationParam) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        List<Group> groups = groupService
            .findAll(executionContext)
            .stream()
            .map(group -> new Group().id(group.getId()).name(group.getName()))
            .collect(Collectors.toList());
        return createListResponse(executionContext, groups, paginationParam);
    }

    @GET
    @Path("/{groupId}/members")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.READ) })
    public Response getMembersByGroupId(@PathParam("groupId") String groupId, @BeanParam PaginationParam paginationParam) {
        //check that group exists
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        groupService.findById(executionContext, groupId);

        List<Member> groupsMembers = membershipService
            .getMembersByReference(executionContext, MembershipReferenceType.GROUP, groupId)
            .stream()
            .filter(
                groupMemberEntity ->
                    groupMemberEntity != null &&
                    groupMemberEntity
                        .getRoles()
                        .stream()
                        .anyMatch(role -> role.getScope().equals(RoleScope.APPLICATION))
            )
            .map(groupMemberEntity -> memberMapper.convert(executionContext, groupMemberEntity, uriInfo))
            .collect(toList());

        return createListResponse(executionContext, groupsMembers, paginationParam);
    }
}

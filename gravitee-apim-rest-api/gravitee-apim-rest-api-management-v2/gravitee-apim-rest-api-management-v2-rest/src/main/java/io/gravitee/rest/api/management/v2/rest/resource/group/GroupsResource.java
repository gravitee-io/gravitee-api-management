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

import io.gravitee.rest.api.management.v2.rest.mapper.GroupMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.GroupsResponse;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Path("/environments/{envId}/groups")
public class GroupsResource extends AbstractResource {

    @Inject
    private GroupService groupService;

    private final GroupMapper mapper = GroupMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { RolePermissionAction.READ }) })
    public GroupsResponse listGroups(@BeanParam @Valid PaginationParam paginationParam) {
        List<GroupEntity> groups = groupService.findAll(GraviteeContext.getExecutionContext());

        List<GroupEntity> groupsSubset = computePaginationData(groups, paginationParam);

        return new GroupsResponse()
            .data(mapper.map(groupsSubset))
            .pagination(PaginationInfo.computePaginationInfo(groups.size(), groupsSubset.size(), paginationParam))
            .links(computePaginationLinks(groups.size(), paginationParam));
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
}

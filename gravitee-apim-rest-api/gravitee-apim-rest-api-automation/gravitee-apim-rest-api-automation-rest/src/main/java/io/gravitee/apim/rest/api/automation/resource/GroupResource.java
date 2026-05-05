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
package io.gravitee.apim.rest.api.automation.resource;

import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.rest.api.automation.exception.HRIDNotFoundException;
import io.gravitee.apim.rest.api.automation.mapper.GroupMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupResource extends AbstractResource {

    @Inject
    private GroupQueryService groupQueryService;

    @Inject
    private GroupService groupService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = { RolePermissionAction.READ }) })
    public Response getGroupByHRID(@PathParam("hrid") String hrid, @QueryParam("hridContainsUUID") boolean hridContainsUUID) {
        var executionContext = GraviteeContext.getExecutionContext();
        var groupId = hridContainsUUID ? hrid : HRIDToUUID.group().context(executionContext).hrid(hrid).id();
        var group = groupQueryService.findById(groupId).orElseThrow(() -> new HRIDNotFoundException(hrid));

        Set<MemberEntity> memberEntities = membershipService.getMembersByReference(
            executionContext,
            MembershipReferenceType.GROUP,
            groupId
        );

        var members = buildGroupMembers(executionContext, memberEntities);

        return Response.ok(GroupMapper.INSTANCE.groupToGroupState(group, members, executionContext)).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GROUP, acls = RolePermissionAction.DELETE) })
    public Response deleteGroupByHrid(@PathParam("hrid") String hrid, @QueryParam("hridContainsUUID") boolean hridContainsUUID) {
        var executionContext = GraviteeContext.getExecutionContext();
        try {
            String groupId = hridContainsUUID ? hrid : HRIDToUUID.group().context(executionContext).hrid(hrid).id();
            groupService.delete(executionContext, groupId);
        } catch (GroupNotFoundException e) {
            throw new HRIDNotFoundException(hrid);
        }
        return Response.noContent().build();
    }

    private SequencedSet<GroupCRDSpec.Member> buildGroupMembers(ExecutionContext executionContext, Set<MemberEntity> memberEntities) {
        if (memberEntities.isEmpty()) {
            return new LinkedHashSet<>();
        }

        var memberRolesById = new LinkedHashMap<String, Map<RoleScope, String>>();

        for (MemberEntity member : memberEntities) {
            var roles = memberRolesById.computeIfAbsent(member.getId(), k -> new LinkedHashMap<>());

            Optional.ofNullable(member.getRoles())
                .orElse(List.of())
                .forEach(role -> roles.put(RoleScope.valueOf(role.getScope().name()), role.getName()));
        }

        var usersByIds = userService
            .findByIds(executionContext, memberRolesById.keySet())
            .stream()
            .collect(Collectors.groupingBy(UserEntity::getId));

        return memberRolesById
            .keySet()
            .stream()
            .map(usersByIds::get)
            .map(List::getFirst)
            .map(user ->
                GroupCRDSpec.Member.builder()
                    .source(user.getSource())
                    .sourceId(user.getSourceId())
                    .roles(memberRolesById.getOrDefault(user.getId(), Map.of()))
                    .build()
            )
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

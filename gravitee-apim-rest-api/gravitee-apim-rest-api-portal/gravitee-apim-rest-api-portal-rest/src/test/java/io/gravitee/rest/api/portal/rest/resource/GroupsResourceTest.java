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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "groups";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        when(permissionService.hasPermission(any(), eq(RolePermission.ENVIRONMENT_GROUP), any(), any())).thenReturn(false);
    }

    @Test
    void should_return_members_when_the_group_is_attached_to_the_caller_application() {
        when(groupService.findGroupIdsAttachedToUserResources(any(), eq(USER_NAME))).thenReturn(Set.of("group-1"));
        when(groupService.findById(any(), eq("group-1"))).thenReturn(GroupEntity.builder().id("group-1").name("Group 1").build());
        when(membershipService.getMembersByReference(any(), eq(MembershipReferenceType.GROUP), eq("group-1"))).thenReturn(Set.of());

        Response response = target("/group-1/members").request().get();

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void should_reject_members_of_a_group_the_caller_did_not_already_have() {
        when(groupService.findGroupIdsAttachedToUserResources(any(), eq(USER_NAME))).thenReturn(Set.of("group-1"));

        Response response = target("/foreign-group/members").request().get();

        org.junit.jupiter.api.Assertions.assertEquals(403, response.getStatus());
    }
}

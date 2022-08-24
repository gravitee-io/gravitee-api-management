/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.v4.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PrimaryOwnerNotFoundException;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PrimaryOwnerServiceImplTest {

    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private RoleService roleService;

    @Before
    public void setUp() {
        this.primaryOwnerService = new PrimaryOwnerServiceImpl(userService, membershipService, groupService, parameterService, roleService);
    }

    @Test
    public void getPrimaryOwner_should_throw_PrimaryOwnerNotFoundException_if_no_membership_found() {
        when(
            membershipService.getPrimaryOwner(
                GraviteeContext.getExecutionContext().getOrganizationId(),
                MembershipReferenceType.API,
                "my-api"
            )
        )
            .thenReturn(null);
        PrimaryOwnerNotFoundException exception = assertThrows(
            PrimaryOwnerNotFoundException.class,
            () -> primaryOwnerService.getPrimaryOwner(GraviteeContext.getExecutionContext(), "my-api")
        );
        assertEquals("Primary owner not found for API [my-api]", exception.getMessage());
        assertEquals("primaryOwner.notFound", exception.getTechnicalCode());
        assertEquals(500, exception.getHttpStatusCode());
        assertEquals(Map.of("api", "my-api"), exception.getParameters());
    }

    @Test
    public void shouldReturnPrimaryOwners() {
        List<String> apiIds = List.of("api1", "api2", "api3");

        RoleEntity poRole = new RoleEntity();
        poRole.setId("API_PRIMARY_OWNER");
        when(roleService.findPrimaryOwnerRoleByOrganization(any(), any())).thenReturn(poRole);

        UserEntity admin = new UserEntity();
        admin.setId("admin");
        UserEntity user = new UserEntity();
        user.setId("user");
        GroupEntity group = new GroupEntity();
        group.setId("group");

        MemberEntity poMember = new MemberEntity();
        poMember.setId(admin.getId());
        poMember.setReferenceId("api1");
        poMember.setRoles(Collections.singletonList(poRole));
        poMember.setType(MembershipMemberType.USER);

        MemberEntity poMember2 = new MemberEntity();
        poMember2.setId(user.getId());
        poMember2.setReferenceId("api2");
        poMember2.setType(MembershipMemberType.USER);

        MemberEntity poMember3 = new MemberEntity();
        poMember3.setId(group.getId());
        poMember3.setReferenceId("api3");
        poMember3.setType(MembershipMemberType.GROUP);
        when(
            membershipService.getMembersByReferencesAndRole(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                apiIds,
                "API_PRIMARY_OWNER"
            )
        )
            .thenReturn(new LinkedHashSet<>(Arrays.asList(poMember, poMember2, poMember3)));

        when(
            userService.findByIds(
                eq(GraviteeContext.getExecutionContext()),
                argThat(argument -> argument.containsAll(Set.of(admin.getId(), user.getId())))
            )
        )
            .thenReturn(new LinkedHashSet<>(List.of(admin, user)));
        when(groupService.findByIds(new LinkedHashSet<>(List.of(group.getId())))).thenReturn(new LinkedHashSet<>(List.of(group)));

        Map<String, PrimaryOwnerEntity> primaryOwners = primaryOwnerService.getPrimaryOwners(GraviteeContext.getExecutionContext(), apiIds);
        assertNotNull(primaryOwners);
        assertEquals(3, primaryOwners.size());
    }
}

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
package io.gravitee.management.service;

import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.impl.MembershipServiceImpl;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static java.util.Optional.of;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipService_GetMembersTest {

    private static final String API_ID = "api-id-1";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Test
    public void shouldGetEmptyMembersWithMembership() throws Exception {
        when(membershipRepository.findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.emptySet());

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name());

        Assert.assertNotNull(members);
        Assert.assertTrue("members must be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name());
    }

    @Test
    public void shouldGetMembersWithMembership() throws Exception {
        Membership membership = new Membership();
        membership.setReferenceId(API_ID);
        membership.setCreatedAt(new Date());
        membership.setUpdatedAt(membership.getCreatedAt());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        membership.setUserId("user-id");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(membership.getUserId());
        userEntity.setFirstname("John");
        userEntity.setLastname("Doe");
        RoleEntity po = mock(RoleEntity.class);
        po.setScope(io.gravitee.management.model.permissions.RoleScope.API);
        po.setName(SystemRole.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(membership));
        when(userService.findById(membership.getUserId())).thenReturn(userEntity);
        when(membershipRepository.findById(userEntity.getId(),MembershipReferenceType.API, API_ID)).thenReturn(of(membership));
        when(roleService.findById(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(po);

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name());

        Assert.assertNotNull(members);
        Assert.assertFalse("members must not be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, SystemRole.PRIMARY_OWNER.name());
        verify(userService, times(1)).findById(membership.getUserId());
    }

    @Test
    public void shouldGetMembersWithoutMembership() throws Exception {
        Membership membership = new Membership();
        membership.setReferenceId(API_ID);
        membership.setCreatedAt(new Date());
        membership.setUpdatedAt(membership.getCreatedAt());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
        membership.setUserId("user-id");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(membership.getUserId());
        userEntity.setFirstname("John");
        userEntity.setLastname("Doe");
        RoleEntity po = mock(RoleEntity.class);
        po.setScope(io.gravitee.management.model.permissions.RoleScope.API);
        po.setName(SystemRole.PRIMARY_OWNER.name());
        when(membershipRepository.findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, null))
                .thenReturn(Collections.singleton(membership));
        when(userService.findById(membership.getUserId())).thenReturn(userEntity);
        when(membershipRepository.findById(userEntity.getId(),MembershipReferenceType.API, API_ID)).thenReturn(of(membership));
        when(roleService.findById(RoleScope.API, SystemRole.PRIMARY_OWNER.name())).thenReturn(po);

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID, RoleScope.API);

        Assert.assertNotNull(members);
        Assert.assertFalse("members must not be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndRole(MembershipReferenceType.API, API_ID, RoleScope.API, null);
        verify(userService, times(1)).findById(membership.getUserId());
    }

}

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
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.NewUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.exceptions.NotAuthorizedMembershipException;
import io.gravitee.management.service.impl.MembershipServiceImpl;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MembershipServiceTest {

    private static final String API_ID = "api-id-1";

    @InjectMocks
    private MembershipService membershipService = new MembershipServiceImpl();

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private IdentityService identityService;

    @Test
    public void shouldGetEmptyMembersWithMembership() throws Exception {
        when(membershipRepository.findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.emptySet());

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER);

        Assert.assertNotNull(members);
        Assert.assertTrue("members must be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER.name());
    }

    @Test
    public void shouldGetMembersWithMembership() throws Exception {
        Membership membership = new Membership();
        membership.setReferenceId(API_ID);
        membership.setCreatedAt(new Date());
        membership.setUpdatedAt(membership.getCreatedAt());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setType(MembershipType.PRIMARY_OWNER.name());
        membership.setUserId("user-id");
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(membership.getUserId());
        userEntity.setFirstname("John");
        userEntity.setLastname("Doe");
        when(membershipRepository.findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER.name()))
                .thenReturn(Collections.singleton(membership));
        when(userService.findByName(membership.getUserId())).thenReturn(userEntity);

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER);

        Assert.assertNotNull(members);
        Assert.assertFalse("members must not be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, MembershipType.PRIMARY_OWNER.name());
        verify(userService, times(1)).findByName(membership.getUserId());
    }

    @Test
    public void shouldGetMembersWithoutMembership() throws Exception {
        Membership membership = new Membership();
        membership.setReferenceId(API_ID);
        membership.setCreatedAt(new Date());
        membership.setUpdatedAt(membership.getCreatedAt());
        membership.setReferenceType(MembershipReferenceType.API);
        membership.setType(MembershipType.PRIMARY_OWNER.name());
        membership.setUserId("user-id");
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(membership.getUserId());
        userEntity.setFirstname("John");
        userEntity.setLastname("Doe");
        when(membershipRepository.findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, null))
                .thenReturn(Collections.singleton(membership));
        when(userService.findByName(membership.getUserId())).thenReturn(userEntity);

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, API_ID);

        Assert.assertNotNull(members);
        Assert.assertFalse("members must not be empty", members.isEmpty());
        verify(membershipRepository, times(1)).findByReferenceAndMembershipType(MembershipReferenceType.API, API_ID, null);
        verify(userService, times(1)).findByName(membership.getUserId());
    }

    @Test
    public void shouldAddApiGroupMembership() throws Exception {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("my name");
        userEntity.setEmail("me@mail.com");

        when(userService.findByName(userEntity.getUsername())).thenReturn(userEntity);
        when(membershipRepository.findById(userEntity.getUsername(), MembershipReferenceType.API_GROUP, API_ID)).thenReturn(Optional.empty());


        membershipService.addOrUpdateMember(MembershipReferenceType.API_GROUP, API_ID, userEntity.getUsername(), MembershipType.OWNER);

        verify(userService, times(1)).findByName(userEntity.getUsername());
        verify(membershipRepository, times(1)).findById(userEntity.getUsername(), MembershipReferenceType.API_GROUP, API_ID);
        verify(membershipRepository, times(1)).create(any());
        verify(membershipRepository, never()).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(any());
    }

    @Test
    public void shouldUpdateApiGroupMembership() throws Exception {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("my name");
        userEntity.setEmail("me@mail.com");
        Membership membership = new Membership();
        membership.setUserId(userEntity.getUsername());

        when(userService.findByName(userEntity.getUsername())).thenReturn(userEntity);
        when(membershipRepository.findById(userEntity.getUsername(), MembershipReferenceType.API_GROUP, API_ID)).thenReturn(Optional.of(membership));

        membershipService.addOrUpdateMember(MembershipReferenceType.API_GROUP, API_ID, userEntity.getUsername(), MembershipType.OWNER);

        verify(userService, times(1)).findByName(userEntity.getUsername());
        verify(membershipRepository, times(1)).findById(userEntity.getUsername(), MembershipReferenceType.API_GROUP, API_ID);
        verify(membershipRepository, never()).create(any());
        verify(membershipRepository, times(1)).update(any());
        verify(emailService, times(1)).sendAsyncEmailNotification(any());
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddPrimaryOwnerOnApiGroup() throws Exception {
        membershipService.addOrUpdateMember(MembershipReferenceType.API_GROUP, API_ID, "xxxxx", MembershipType.PRIMARY_OWNER);
    }

    @Test(expected = NotAuthorizedMembershipException.class)
    public void shouldDisallowAddPrimaryOwnerOnApplicationGroup() throws Exception {
        membershipService.addOrUpdateMember(MembershipReferenceType.APPLICATION_GROUP, API_ID, "xxxxx", MembershipType.PRIMARY_OWNER);
    }
}

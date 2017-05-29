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
package io.gravitee.management.security.listener;

import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationSuccessListenerTest {

    @InjectMocks
    private AuthenticationSuccessListener listener = new AuthenticationSuccessListener();

    @Mock
    private UserService userServiceMock;
    @Mock
    private MembershipService membershipServiceMock;
    @Mock
    private RoleService roleServiceMock;

    @Mock
    private AuthenticationSuccessEvent eventMock;
    @Mock
    private Authentication authenticationMock;
    @Mock
    private UserDetails userDetailsMock;

    @Test
    public void shouldConnectFoundUser() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, never()).create(any(NewExternalUserEntity.class), anyBoolean());
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithDefaultRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        when(authenticationMock.getAuthorities()).thenReturn(null);
        when(userServiceMock.findByName(userDetailsMock.getUsername(), false)).thenThrow(UserNotFoundException.class);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(true));
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomGlobalRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userServiceMock.findByName(userDetailsMock.getUsername(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity = mock(RoleEntity.class);
        when(roleEntity.getName()).thenReturn("ROLE");
        when(roleServiceMock.findById(RoleScope.MANAGEMENT, "ROLE")).thenReturn(roleEntity);
        when(roleServiceMock.findById(RoleScope.PORTAL, "ROLE")).thenReturn(roleEntity);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.MANAGEMENT,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.MANAGEMENT,
                        "ROLE"
                );
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.PORTAL,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.PORTAL,
                        "ROLE"
                );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomSpecificRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Arrays.asList(
                new SimpleGrantedAuthority("MANAGEMENT:ROLE1"),
                new SimpleGrantedAuthority("PORTAL:ROLE2"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userServiceMock.findByName(userDetailsMock.getUsername(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity1 = mock(RoleEntity.class);
        when(roleEntity1.getName()).thenReturn("ROLE1");
        RoleEntity roleEntity2 = mock(RoleEntity.class);
        when(roleEntity2.getName()).thenReturn("ROLE2");
        when(roleServiceMock.findById(RoleScope.MANAGEMENT, "ROLE1")).thenReturn(roleEntity1);
        when(roleServiceMock.findById(RoleScope.PORTAL, "ROLE2")).thenReturn(roleEntity2);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.MANAGEMENT,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.MANAGEMENT,
                        "ROLE1"
                );
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.PORTAL,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.PORTAL,
                        "ROLE2"
                );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomSpecificRoleAndGlobal() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE"),
                new SimpleGrantedAuthority("PORTAL:ROLE2"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userServiceMock.findByName(userDetailsMock.getUsername(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity1 = mock(RoleEntity.class);
        when(roleEntity1.getName()).thenReturn("ROLE");
        RoleEntity roleEntity2 = mock(RoleEntity.class);
        when(roleEntity2.getName()).thenReturn("ROLE2");
        when(roleServiceMock.findById(RoleScope.MANAGEMENT, "ROLE")).thenReturn(roleEntity1);
        when(roleServiceMock.findById(RoleScope.PORTAL, "ROLE2")).thenReturn(roleEntity2);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.MANAGEMENT,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.MANAGEMENT,
                        "ROLE"
                );
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.PORTAL,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.PORTAL,
                        "ROLE2"
                );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithAdminRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Arrays.asList(
                new SimpleGrantedAuthority("MANAGEMENT:ROLE1"),
                new SimpleGrantedAuthority("ADMIN"),
                new SimpleGrantedAuthority("PORTAL:ROLE2"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userServiceMock.findByName(userDetailsMock.getUsername(), false)).thenThrow(UserNotFoundException.class);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findByName(userDetailsMock.getUsername(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.MANAGEMENT,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.MANAGEMENT,
                        "ADMIN"
                );
        verify(membershipServiceMock, times(1)).
                addOrUpdateMember(
                        MembershipReferenceType.PORTAL,
                        MembershipDefaultReferenceId.DEFAULT.name(),
                        userDetailsMock.getUsername(),
                        RoleScope.PORTAL,
                        "ADMIN"
                );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }
}

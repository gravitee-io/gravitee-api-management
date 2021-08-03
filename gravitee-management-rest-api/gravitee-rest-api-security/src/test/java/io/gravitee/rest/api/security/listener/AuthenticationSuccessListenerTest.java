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
package io.gravitee.rest.api.security.listener;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

    @Mock
    private UserEntity userEntity;

    private static final String USERSOURCE = "usersource";
    private static final String USERSOURCEID = "usersourceid";
    private static final String USERNAME = "username";

    @Test
    public void shouldConnectFoundUser() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        when(userServiceMock.findBySource(userDetailsMock.getSource(), userDetailsMock.getSourceId(), false)).thenReturn(new UserEntity());

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, never()).create(any(NewExternalUserEntity.class), anyBoolean());
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithDefaultRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        when(userDetailsMock.getUsername()).thenReturn(USERNAME);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        when(authenticationMock.getAuthorities()).thenReturn(null);
        when(userServiceMock.findBySource(userDetailsMock.getSource(), userDetailsMock.getSourceId(), false))
            .thenThrow(UserNotFoundException.class);
        when(userServiceMock.create(any(NewExternalUserEntity.class), eq(true))).thenReturn(userEntity);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(true));
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomGlobalRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        when(userServiceMock.findBySource(USERSOURCE, userDetailsMock.getSourceId(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity = mock(RoleEntity.class);
        when(roleEntity.getName()).thenReturn("ROLE");
        when(roleServiceMock.findByScopeAndName(RoleScope.ENVIRONMENT, "ROLE")).thenReturn(Optional.of(roleEntity));
        when(roleServiceMock.findByScopeAndName(RoleScope.ORGANIZATION, "ROLE")).thenReturn(Optional.of(roleEntity));
        when(userServiceMock.create(any(NewExternalUserEntity.class), eq(false))).thenReturn(userEntity);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1))
            .addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, "DEFAULT"),
                new MembershipService.MembershipMember(userDetailsMock.getUsername(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "ROLE")
            );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomSpecificRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Arrays.asList(
            new SimpleGrantedAuthority("ENVIRONMENT:ROLE1"),
            new SimpleGrantedAuthority("ORGANIZATION:ROLE2")
        );
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        when(userServiceMock.findBySource(USERSOURCE, userDetailsMock.getSourceId(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity1 = mock(RoleEntity.class);
        when(roleEntity1.getName()).thenReturn("ROLE1");
        RoleEntity roleEntity2 = mock(RoleEntity.class);
        when(roleEntity2.getName()).thenReturn("ROLE2");
        when(roleServiceMock.findByScopeAndName(RoleScope.ENVIRONMENT, "ROLE1")).thenReturn(Optional.of(roleEntity1));
        when(roleServiceMock.findByScopeAndName(RoleScope.ORGANIZATION, "ROLE2")).thenReturn(Optional.of(roleEntity2));
        when(userServiceMock.create(any(NewExternalUserEntity.class), eq(false))).thenReturn(userEntity);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1))
            .addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, "DEFAULT"),
                new MembershipService.MembershipMember(userDetailsMock.getUsername(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "ROLE1")
            );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithCustomSpecificRoleAndGlobal() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        Collection authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE"), new SimpleGrantedAuthority("ORGANIZATION:ROLE2"));
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        when(userServiceMock.findBySource(USERSOURCE, userDetailsMock.getSourceId(), false)).thenThrow(UserNotFoundException.class);
        RoleEntity roleEntity1 = mock(RoleEntity.class);
        when(roleEntity1.getName()).thenReturn("ROLE");
        RoleEntity roleEntity2 = mock(RoleEntity.class);
        when(roleEntity2.getName()).thenReturn("ROLE2");
        when(roleServiceMock.findByScopeAndName(RoleScope.ENVIRONMENT, "ROLE")).thenReturn(Optional.of(roleEntity1));
        when(roleServiceMock.findByScopeAndName(RoleScope.ORGANIZATION, "ROLE2")).thenReturn(Optional.of(roleEntity2));
        when(userServiceMock.create(any(NewExternalUserEntity.class), eq(false))).thenReturn(userEntity);

        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1))
            .addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, "DEFAULT"),
                new MembershipService.MembershipMember(userDetailsMock.getUsername(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "ROLE")
            );
        verify(userServiceMock, times(1)).connect(userDetailsMock.getUsername());
    }

    @Test
    public void shouldCreateUserWithAdminRole() {
        when(eventMock.getAuthentication()).thenReturn(authenticationMock);
        when(authenticationMock.getPrincipal()).thenReturn(userDetailsMock);
        when(userDetailsMock.getSource()).thenReturn(USERSOURCE);
        when(userDetailsMock.getSourceId()).thenReturn(USERSOURCEID);
        Collection authorities = Arrays.asList(
            new SimpleGrantedAuthority("MANAGEMENT:ROLE1"),
            new SimpleGrantedAuthority("ADMIN"),
            new SimpleGrantedAuthority("PORTAL:ROLE2")
        );
        when(authenticationMock.getAuthorities()).thenReturn(authorities);
        when(userServiceMock.findBySource(userDetailsMock.getSource(), userDetailsMock.getSourceId(), false))
            .thenThrow(UserNotFoundException.class);
        when(userServiceMock.create(any(NewExternalUserEntity.class), eq(false))).thenReturn(userEntity);
        listener.onApplicationEvent(eventMock);

        verify(userServiceMock, times(1)).findBySource(USERSOURCE, userDetailsMock.getSourceId(), false);
        verify(userServiceMock, times(1)).create(any(NewExternalUserEntity.class), eq(false));
        verify(membershipServiceMock, times(1))
            .addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, "DEFAULT"),
                new MembershipService.MembershipMember(userDetailsMock.getUsername(), null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, "ADMIN")
            );
    }

    @Test
    public void shouldReturnPictureURL() {
        String pictureURL = "https://my.photo";
        String picture = listener.computePicture(pictureURL.getBytes());

        assertEquals(pictureURL, picture);
    }

    @Test
    public void shouldReturnPictureContent() throws URISyntaxException, IOException {
        Path resourcePath = Paths.get(getClass().getResource("beer.jpeg").toURI());
        byte[] pictureData = Files.readAllBytes(resourcePath);
        String picture = listener.computePicture(pictureData);

        String expectedPictureContent =
            "data:image/jpeg;base64,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";

        assertEquals(expectedPictureContent, picture);
    }
}

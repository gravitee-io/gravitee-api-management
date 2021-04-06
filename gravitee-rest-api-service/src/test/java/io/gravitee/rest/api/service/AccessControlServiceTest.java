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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.AccessControlServiceImpl;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessControlServiceTest {

    public static final String USERNAME = "johndoe";
    private static final String ROLE_ID = "my-role";
    private static final String GROUP_ID = "my-group";

    @InjectMocks
    private AccessControlService accessControlService = new AccessControlServiceImpl();

    @Mock
    private MembershipService membershipServiceMock;

    @Mock
    private ApiService apiService;

    @Mock
    private RoleService roleService;

    @Mock
    private GroupService groupService;

    @Test
    public void shouldAccessPublicApiAsAnonymous() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getVisibility()).thenReturn(Visibility.PUBLIC);

        boolean canAccess = accessControlService.canAccessApiFromPortal(apiEntityMock);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPublicApiAsAuthenticatedUser(){
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getVisibility()).thenReturn(Visibility.PUBLIC);
        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(apiEntityMock);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsAnonymous() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getVisibility()).thenReturn(Visibility.PRIVATE);

        boolean canAccess = accessControlService.canAccessApiFromPortal(apiEntityMock);

        assertFalse(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsAuthenticatedUser() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getVisibility()).thenReturn(Visibility.PRIVATE);
        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(apiEntityMock);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPrivateApiAsAuthenticatedUser() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getVisibility()).thenReturn(Visibility.PRIVATE);

        when(apiService.findPublishedByUser(any(), any())).thenReturn(new HashSet(Collections.singleton(apiEntityMock)));
        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(apiEntityMock);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsNotApiMember() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        doReturn(null).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();

        boolean canAccess = accessControlService.canAccessPageFromConsole(apiEntityMock, pageEntity);

        assertFalse(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, never()).getUserMember(eq(MembershipReferenceType.GROUP), any(), any());
    }

    @Test
    public void shouldAccessPrivateApiAsApiMember() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        MemberEntity memberEntity = mock(MemberEntity.class);
        doReturn(memberEntity).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();
        when(roleService.hasPermission(memberEntity.getPermissions(), ApiPermission.DOCUMENTATION,
            new RolePermissionAction[]{RolePermissionAction.UPDATE, RolePermissionAction.CREATE,
                RolePermissionAction.DELETE})).thenReturn(true);

        boolean canAccess = accessControlService.canAccessPageFromConsole(apiEntityMock, pageEntity);

        assertTrue(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, never()).getUserMember(eq(MembershipReferenceType.GROUP), any(), any());
    }


    @Test
    public void shouldAccessPrivateApiAsGroupApiMember() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        MemberEntity memberEntity = mock(MemberEntity.class);
        when(apiEntityMock.getGroups()).thenReturn(Collections.singleton("groupid"));
        doReturn(null).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        doReturn(memberEntity).when(membershipServiceMock).getUserMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME));

        when(roleService.hasPermission(memberEntity.getPermissions(), ApiPermission.DOCUMENTATION,
            new RolePermissionAction[]{RolePermissionAction.UPDATE, RolePermissionAction.CREATE,
                RolePermissionAction.DELETE})).thenReturn(true);

        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();

        boolean canAccess = accessControlService.canAccessPageFromConsole(apiEntityMock, pageEntity);

        assertTrue(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.API), any(), eq(USERNAME));
        verify(membershipServiceMock, times(1)).getUserMember(eq(MembershipReferenceType.GROUP), any(), eq(USERNAME));
    }


    @Test
    public void shouldNotAccessPublicApiAndUnpublishedPageAsAnonymous() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(false);

        boolean canAccess = accessControlService.canAccessPageFromConsole(apiEntityMock, pageEntity);

        assertFalse(canAccess);
    }


    @Test
    public void shouldNotAccessPageFromPortalWithoutRole() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = new HashSet<>();
        AccessControlEntity accessControlEntity = new AccessControlEntity();
        accessControlEntity.setReferenceId(ROLE_ID);
        accessControlEntity.setReferenceType("ROLE");
        accessControls.add(accessControlEntity);
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithRole() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList("no-role", ROLE_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(membershipServiceMock.getRoles(MembershipReferenceType.ENVIRONMENT,
            GraviteeContext.getCurrentEnvironment(),
            MembershipMemberType.USER, USERNAME)).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithRoleAndExcludedRole() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(pageEntity.isExcludedAccessControls()).thenReturn(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList("no-role", ROLE_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(membershipServiceMock.getRoles(MembershipReferenceType.ENVIRONMENT,
            GraviteeContext.getCurrentEnvironment(),
            MembershipMemberType.USER, USERNAME)).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageFromPortalWithExcludedRole() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(pageEntity.isExcludedAccessControls()).thenReturn(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList(ROLE_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(membershipServiceMock.getRoles(MembershipReferenceType.ENVIRONMENT,
            GraviteeContext.getCurrentEnvironment(),
            MembershipMemberType.USER, USERNAME)).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithGroup() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList("no-role"), Arrays.asList("no-group", GROUP_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithGroupAndExcludedGroup() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.isExcludedAccessControls()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList("no-role"), Arrays.asList("no-group", GROUP_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageFromPortalWithExcludedGroup() {
        PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.isExcludedAccessControls()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(Collections.emptyList(), Arrays.asList(GROUP_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessApiPageFromConsoleWithRole() {
        final ApiEntity apiEntityMock = mock(ApiEntity.class);
        when(apiEntityMock.getGroups()).thenReturn(Collections.singleton(GROUP_ID));

        final PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.isPublished()).thenReturn(true);
        when(pageEntity.getVisibility()).thenReturn(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(Arrays.asList(ROLE_ID), Arrays.asList("no-group", GROUP_ID));
        when(pageEntity.getAccessControls()).thenReturn(accessControls);
        connectUser();

        MemberEntity memberEntity = mock(MemberEntity.class);
        List<RoleEntity> roles = new ArrayList<>();
        RoleEntity roleEntity = mock(RoleEntity.class);
        when(roleEntity.getId()).thenReturn(ROLE_ID);
        roles.add(roleEntity);
        when(memberEntity.getRoles()).thenReturn(roles);
        when(membershipServiceMock.getUserMember(MembershipReferenceType.GROUP, GROUP_ID, USERNAME)).thenReturn(memberEntity);

        boolean canAccess = accessControlService.canAccessPageFromConsole(apiEntityMock, pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageWithSpecialTypes() {
        final PageEntity pageEntity = mock(PageEntity.class);
        when(pageEntity.getType()).thenReturn(PageType.MARKDOWN_TEMPLATE.name());

        boolean canAccess = accessControlService.canAccessPageFromPortal(pageEntity);

        assertFalse(canAccess);
    }

    private Set<AccessControlEntity> buildAccessControls(List<String> roles) {
        return buildAccessControls(roles, Collections.emptyList());
    }

    private Set<AccessControlEntity> buildAccessControls(List<String> roles, List<String> groups) {
        Set<AccessControlEntity> accessControlEntities = new HashSet<>();
        Set<AccessControlEntity> roleEntities = roles.stream().map(id -> {
            AccessControlEntity accessControlEntity = new AccessControlEntity();
            accessControlEntity.setReferenceId(id);
            accessControlEntity.setReferenceType("ROLE");
            return accessControlEntity;
        }).collect(Collectors.toSet());

        accessControlEntities.addAll(roleEntities);

        Set<AccessControlEntity> groupEntities = groups.stream().map(id -> {
            AccessControlEntity accessControlEntity = new AccessControlEntity();
            accessControlEntity.setReferenceId(id);
            accessControlEntity.setReferenceType("GROUP");
            return accessControlEntity;
        }).collect(Collectors.toSet());

        accessControlEntities.addAll(groupEntities);

        return accessControlEntities;
    }

    public static void connectUser() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserDetails(USERNAME, "password", Collections.emptyList());
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return false;
                    }

                    @Override
                    public void setAuthenticated(boolean b) throws IllegalArgumentException {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
    }


    @After
    public void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return null;
            }
            @Override
            public void setAuthentication(Authentication authentication) {
            }
        });
    }
}

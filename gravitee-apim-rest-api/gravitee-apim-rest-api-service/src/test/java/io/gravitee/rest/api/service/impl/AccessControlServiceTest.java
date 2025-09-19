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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.AccessControlEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.AccessControlService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String API_ID = "my-api";

    @InjectMocks
    private AccessControlService accessControlService = new AccessControlServiceImpl();

    @Mock
    private MembershipService membershipServiceMock;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private RoleService roleService;

    @Mock
    private GroupService groupService;

    public static void connectUser() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
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
                        public void setAuthenticated(boolean b) throws IllegalArgumentException {}

                        @Override
                        public String getName() {
                            return null;
                        }
                    };
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Test
    public void shouldAccessPublicApiAsAnonymous() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);

        boolean canAccess = accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPublicApiAsAuthenticatedUser() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsAnonymous() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setVisibility(Visibility.PRIVATE);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        boolean canAccess = accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsAuthenticatedUser() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setVisibility(Visibility.PRIVATE);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPrivateApiAsAuthenticatedUser() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        apiEntity.setVisibility(Visibility.PRIVATE);
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);

        when(
            apiAuthorizationService.canConsumeApi(eq(GraviteeContext.getExecutionContext()), any(String.class), any(GenericApiEntity.class))
        ).thenReturn(true);

        connectUser();

        boolean canAccess = accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), apiEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPrivateApiAsNotApiMember() {
        final ApiEntity apiEntity = new ApiEntity();
        when(
            membershipServiceMock.getUserMember(
                eq(GraviteeContext.getExecutionContext()),
                eq(MembershipReferenceType.API),
                any(),
                eq(USERNAME)
            )
        ).thenReturn(null);
        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();

        boolean canAccess = accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiEntity, pageEntity);

        assertFalse(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.API),
            any(),
            eq(USERNAME)
        );
        verify(membershipServiceMock, never()).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.GROUP),
            any(),
            any()
        );
    }

    @Test
    public void shouldAccessPrivateApiAsApiMember() {
        final ApiEntity apiEntity = new ApiEntity();
        MemberEntity memberEntity = new MemberEntity();

        when(
            membershipServiceMock.getUserMember(
                eq(GraviteeContext.getExecutionContext()),
                eq(MembershipReferenceType.API),
                any(),
                eq(USERNAME)
            )
        ).thenReturn(memberEntity);
        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();
        when(
            roleService.hasPermission(
                memberEntity.getPermissions(),
                ApiPermission.DOCUMENTATION,
                new RolePermissionAction[] { RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE }
            )
        ).thenReturn(true);

        boolean canAccess = accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiEntity, pageEntity);

        assertTrue(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.API),
            any(),
            eq(USERNAME)
        );
        verify(membershipServiceMock, never()).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.GROUP),
            any(),
            any()
        );
    }

    @Test
    public void shouldAccessPrivateApiAsGroupApiMember() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setGroups(Collections.singleton("groupid"));
        MemberEntity memberEntity = new MemberEntity();
        when(
            membershipServiceMock.getUserMember(
                eq(GraviteeContext.getExecutionContext()),
                eq(MembershipReferenceType.API),
                any(),
                eq(USERNAME)
            )
        ).thenReturn(null);
        when(
            membershipServiceMock.getUserMember(
                eq(GraviteeContext.getExecutionContext()),
                eq(MembershipReferenceType.GROUP),
                any(),
                eq(USERNAME)
            )
        ).thenReturn(memberEntity);

        when(
            roleService.hasPermission(
                memberEntity.getPermissions(),
                ApiPermission.DOCUMENTATION,
                new RolePermissionAction[] { RolePermissionAction.UPDATE, RolePermissionAction.CREATE, RolePermissionAction.DELETE }
            )
        ).thenReturn(true);

        final PageEntity pageEntity = mock(PageEntity.class);
        connectUser();

        boolean canAccess = accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiEntity, pageEntity);

        assertTrue(canAccess);
        verify(membershipServiceMock, times(1)).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.API),
            any(),
            eq(USERNAME)
        );
        verify(membershipServiceMock, times(1)).getUserMember(
            eq(GraviteeContext.getExecutionContext()),
            eq(MembershipReferenceType.GROUP),
            any(),
            eq(USERNAME)
        );
    }

    @Test
    public void shouldNotAccessPublicApiAndUnpublishedPageAsAnonymous() {
        final ApiEntity apiEntity = new ApiEntity();
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(false);

        boolean canAccess = accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiEntity, pageEntity);

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

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithRole() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of("no-role", ROLE_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(
            membershipServiceMock.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                GraviteeContext.getCurrentEnvironment(),
                MembershipMemberType.USER,
                USERNAME
            )
        ).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithRoleAndExcludedRole() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        pageEntity.setExcludedAccessControls(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of("no-role", ROLE_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(
            membershipServiceMock.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                GraviteeContext.getCurrentEnvironment(),
                MembershipMemberType.USER,
                USERNAME
            )
        ).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageFromPortalWithExcludedRole() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        pageEntity.setExcludedAccessControls(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of(ROLE_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        when(
            membershipServiceMock.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                GraviteeContext.getCurrentEnvironment(),
                MembershipMemberType.USER,
                USERNAME
            )
        ).thenReturn(new HashSet(Collections.singleton(roleEntity)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithGroup() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of("no-role"), List.of("no-group", GROUP_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessPageFromPortalWithGroupAndExcludedGroup() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        pageEntity.setExcludedAccessControls(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of("no-role"), List.of("no-group", GROUP_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageFromPortalWithExcludedGroup() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        pageEntity.setExcludedAccessControls(true);
        Set<AccessControlEntity> accessControls = buildAccessControls(Collections.emptyList(), List.of(GROUP_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        GroupEntity myGroup = new GroupEntity();
        myGroup.setId(GROUP_ID);
        when(groupService.findByUser(USERNAME)).thenReturn(new HashSet<>(Collections.singleton(myGroup)));

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertFalse(canAccess);
    }

    @Test
    public void shouldAccessApiPageFromConsoleWithRole() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setGroups(Collections.singleton(GROUP_ID));

        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of(ROLE_ID), List.of("no-group", GROUP_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        List<RoleEntity> roles = new ArrayList<>();
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        roles.add(roleEntity);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setRoles(roles);
        when(
            membershipServiceMock.getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP_ID, USERNAME)
        ).thenReturn(memberEntity);

        boolean canAccess = accessControlService.canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiEntity, pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldAccessApiPageFromPortalWithRole() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);

        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiEntity);

        PageEntity pageEntity = new PageEntity();
        pageEntity.setPublished(true);
        pageEntity.setVisibility(Visibility.PRIVATE);
        Set<AccessControlEntity> accessControls = buildAccessControls(List.of(ROLE_ID));
        pageEntity.setAccessControls(accessControls);
        connectUser();

        Set<RoleEntity> roles = new HashSet<>();
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(ROLE_ID);
        roles.add(roleEntity);
        when(membershipServiceMock.getRoles(MembershipReferenceType.API, API_ID, MembershipMemberType.USER, USERNAME)).thenReturn(roles);

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), API_ID, pageEntity);

        assertTrue(canAccess);
    }

    @Test
    public void shouldNotAccessPageWithSpecialTypes() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setType(PageType.MARKDOWN_TEMPLATE.name());

        boolean canAccess = accessControlService.canAccessPageFromPortal(GraviteeContext.getExecutionContext(), pageEntity);

        assertFalse(canAccess);
    }

    private Set<AccessControlEntity> buildAccessControls(List<String> roles) {
        return buildAccessControls(roles, Collections.emptyList());
    }

    private Set<AccessControlEntity> buildAccessControls(List<String> roles, List<String> groups) {
        Set<AccessControlEntity> accessControlEntities = new HashSet<>();
        Set<AccessControlEntity> roleEntities = roles
            .stream()
            .map(id -> {
                AccessControlEntity accessControlEntity = new AccessControlEntity();
                accessControlEntity.setReferenceId(id);
                accessControlEntity.setReferenceType("ROLE");
                return accessControlEntity;
            })
            .collect(Collectors.toSet());

        accessControlEntities.addAll(roleEntities);

        Set<AccessControlEntity> groupEntities = groups
            .stream()
            .map(id -> {
                AccessControlEntity accessControlEntity = new AccessControlEntity();
                accessControlEntity.setReferenceId(id);
                accessControlEntity.setReferenceType("GROUP");
                return accessControlEntity;
            })
            .collect(Collectors.toSet());

        accessControlEntities.addAll(groupEntities);

        return accessControlEntities;
    }

    @After
    public void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }
}

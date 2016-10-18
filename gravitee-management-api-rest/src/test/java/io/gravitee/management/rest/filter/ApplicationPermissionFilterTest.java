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
package io.gravitee.management.rest.filter;

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.*;

import static javax.swing.UIManager.put;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class ApplicationPermissionFilterTest {

    @InjectMocks
    protected ApplicationPermissionFilter applicationPermissionFilter;

    @Mock
    protected ApplicationService applicationService;

    @Mock
    protected ResourceInfo resourceInfo;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected ApplicationPermissionsRequired applicationPermissionsRequired;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithoutGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(applicationPermissionsRequired.value()).thenReturn(ApplicationPermission.READ);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getMember(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(null);

        try {
            applicationPermissionFilter.filter(applicationPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        application.setGroup(new GroupEntity());
        application.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(applicationPermissionsRequired.value()).thenReturn(ApplicationPermission.READ);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getMember(any(MembershipReferenceType.class), anyString(), eq(user.getName()))).thenReturn(null);

        try {
            applicationPermissionFilter.filter(applicationPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(2)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsInsufficient() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(applicationPermissionsRequired.value()).thenReturn(ApplicationPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.USER);
        when(membershipService.getMember(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(memberEntity);

        try {
            applicationPermissionFilter.filter(applicationPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithoutGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(applicationPermissionsRequired.value()).thenReturn(ApplicationPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.OWNER);
        when(membershipService.getMember(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(memberEntity);

        applicationPermissionFilter.filter(applicationPermissionsRequired, containerRequestContext);
        verify(membershipService, times(1)).getMember(any(), any(), any());
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithGroup() {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-app");
        application.setGroup(new GroupEntity());
        application.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(applicationService.findById(application.getId())).thenReturn(application);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(applicationPermissionsRequired.value()).thenReturn(ApplicationPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("application", Collections.singletonList(application.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.OWNER);
        when(membershipService.getMember(MembershipReferenceType.APPLICATION, application.getId(), user.getName())).thenReturn(null);
        when(membershipService.getMember(MembershipReferenceType.APPLICATION_GROUP, application.getGroup().getId(), user.getName())).thenReturn(memberEntity);

        applicationPermissionFilter.filter(applicationPermissionsRequired, containerRequestContext);
        verify(membershipService, times(2)).getMember(any(), any(), any());
    }
}

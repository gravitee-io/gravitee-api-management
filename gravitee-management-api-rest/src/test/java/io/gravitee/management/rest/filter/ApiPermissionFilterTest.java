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

import io.gravitee.management.model.GroupEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.permissions.ApiPermission;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.rest.security.ApiPermissionsRequired;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class ApiPermissionFilterTest {

    @InjectMocks
    protected ApiPermissionFilter apiPermissionFilter;

    @Mock
    protected ApiService apiService;

    @Mock
    protected ResourceInfo resourceInfo;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected MembershipService membershipService;

    @Mock
    protected ApiPermissionsRequired apiPermissionsRequired;

    @Mock
    protected ContainerRequestContext containerRequestContext;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithoutGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(apiPermissionsRequired.value()).thenReturn(ApiPermission.READ);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getMember(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(null);

        try {
            apiPermissionFilter.filter(apiPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsNullWithGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        api.setGroup(new GroupEntity());
        api.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(apiPermissionsRequired.value()).thenReturn(ApiPermission.READ);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        when(membershipService.getMember(any(MembershipReferenceType.class), anyString(), eq(user.getName()))).thenReturn(null);

        try {
            apiPermissionFilter.filter(apiPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(2)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldThrowForbiddenExceptionWhenMembershipIsInsufficient() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(apiPermissionsRequired.value()).thenReturn(ApiPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.USER);
        when(membershipService.getMember(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(memberEntity);

        try {
            apiPermissionFilter.filter(apiPermissionsRequired, containerRequestContext);
        } catch(ForbiddenAccessException e) {
            verify(membershipService, times(1)).getMember(any(), any(), any());
            throw e;
        }

        Assert.fail("Should throw a ForbiddenAccessException");
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithoutGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(apiPermissionsRequired.value()).thenReturn(ApiPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.OWNER);
        when(membershipService.getMember(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(memberEntity);

        apiPermissionFilter.filter(apiPermissionsRequired, containerRequestContext);
        verify(membershipService, times(1)).getMember(any(), any(), any());
    }

    @Test
    public void shouldAllowUserWithSufficientPermissionsWithGroup() {
        ApiEntity api = new ApiEntity();
        api.setId("my-api");
        api.setGroup(new GroupEntity());
        api.getGroup().setId("my-group");
        Principal user = () -> "user";
        when(apiService.findById(api.getId())).thenReturn(api);
        when(securityContext.getUserPrincipal()).thenReturn(user);
        when(apiPermissionsRequired.value()).thenReturn(ApiPermission.MANAGE_MEMBERS);
        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        map.put("api", Collections.singletonList(api.getId()));
        when(uriInfo.getPathParameters()).thenReturn(map);
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setType(MembershipType.OWNER);
        when(membershipService.getMember(MembershipReferenceType.API, api.getId(), user.getName())).thenReturn(null);
        when(membershipService.getMember(MembershipReferenceType.API_GROUP, api.getGroup().getId(), user.getName())).thenReturn(memberEntity);

        apiPermissionFilter.filter(apiPermissionsRequired, containerRequestContext);
        verify(membershipService, times(2)).getMember(any(), any(), any());
    }
}

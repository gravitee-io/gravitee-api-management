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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPagesResourceNotAdminTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String PAGE_NAME = "p";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/pages/" + PAGE_NAME;
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Test
    public void shouldGetPublicApiPublishedPage() {
        reset(apiSearchServiceV4, pageService, membershipService, accessControlService);
        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiSearchServiceV4).findGenericById(GraviteeContext.getExecutionContext(), API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        when(permissionService.hasPermission(any(), any(), any())).thenReturn(true);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);
        doReturn(true).when(accessControlService).canAccessPageFromConsole(GraviteeContext.getExecutionContext(), apiMock, pageMock);

        final Response response = envTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(apiSearchServiceV4, times(1)).findGenericById(GraviteeContext.getExecutionContext(), API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
    }

    @Test
    public void shouldNotGetPrivateApiPublishedPage() {
        reset(apiSearchServiceV4, pageService, membershipService);

        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiSearchServiceV4).findGenericById(GraviteeContext.getExecutionContext(), API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);
        doReturn(true)
            .when(roleService)
            .hasPermission(any(), eq(ApiPermission.DOCUMENTATION), eq(new RolePermissionAction[] { RolePermissionAction.READ }));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        final Response response = envTarget().request().get();

        assertEquals(UNAUTHORIZED_401, response.getStatus());
        verify(apiSearchServiceV4, atLeastOnce()).findGenericById(GraviteeContext.getExecutionContext(), API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
    }
}

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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.management.rest.model.wrapper.ApplicationListItemPagedResult;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Test
    public void shouldNotCreateApplication_noContent() {
        final Response response = envTarget().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApplication_emptyName() {
        final NewApplicationEntity appEntity = new NewApplicationEntity();
        appEntity.setName("");
        appEntity.setDescription("my description");

        ApplicationEntity returnedApp = new ApplicationEntity();
        returnedApp.setId("my-beautiful-application");
        doReturn(returnedApp)
            .when(applicationService)
            .create(eq(GraviteeContext.getExecutionContext()), any(NewApplicationEntity.class), eq(JerseySpringTest.USER_NAME));

        final Response response = envTarget().request().post(Entity.json(appEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateApplication() {
        reset(applicationService);
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setName("My beautiful application");
        newApplicationEntity.setDescription("my description");
        newApplicationEntity.setGroups(Set.of("group1", "group2"));

        ApplicationEntity createdApplication = new ApplicationEntity();
        createdApplication.setId("my-beautiful-application");
        doReturn(createdApplication)
            .when(applicationService)
            .create(eq(GraviteeContext.getExecutionContext()), any(NewApplicationEntity.class), eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(newApplicationEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path("my-beautiful-application").getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldGetApplications() {
        Page<ApplicationListItem> applications = mock(Page.class);
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any())).thenReturn(
            applications
        );

        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldGetApplicationsPaged() {
        var app1 = mock(ApplicationListItem.class);
        when(app1.getId()).thenReturn("app1");
        when(app1.getUpdatedAt()).thenReturn(new Date());
        when(app1.getGroups()).thenReturn(Collections.emptySet());
        var app2 = mock(ApplicationListItem.class);
        when(app2.getId()).thenReturn("app2");
        when(app2.getUpdatedAt()).thenReturn(new Date());
        when(app2.getGroups()).thenReturn(new HashSet<>(Arrays.asList("GROUP1", "GROUP2")));
        var app3 = mock(ApplicationListItem.class);
        when(app3.getId()).thenReturn("app3");
        when(app3.getUpdatedAt()).thenReturn(new Date());
        when(app3.getGroups()).thenReturn(Collections.emptySet());

        List<ApplicationListItem> applications = List.of(app1, app2, app3);
        Page<ApplicationListItem> pagedApplications = new Page(applications, 0, 3, 3);
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any())).thenReturn(
            pagedApplications
        );

        final Response response = envTarget("/_paged").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var responseContent = response.readEntity(ApplicationListItemPagedResult.class);
        var pagedApplicationsResult = (ApplicationListItemPagedResult) responseContent;
        assertEquals(1, pagedApplicationsResult.getPage().getCurrent());
        assertEquals(20, pagedApplicationsResult.getPage().getPerPage());
        assertEquals(3, pagedApplicationsResult.getPage().getSize());
        assertEquals(1, pagedApplicationsResult.getPage().getTotalPages());
        assertEquals(3, pagedApplicationsResult.getPage().getTotalElements());
        Collection<ApplicationListItem> resultApplications = pagedApplicationsResult.getData();
        assertEquals(
            2,
            resultApplications
                .stream()
                .filter(app -> app.getId().equals("app2"))
                .findFirst()
                .get()
                .getGroups()
                .size()
        );
    }

    @Test
    public void shouldGetApplicationsPagedLastPage() {
        var app1 = mock(ApplicationListItem.class);
        when(app1.getId()).thenReturn("app1");
        when(app1.getUpdatedAt()).thenReturn(new Date());

        List<ApplicationListItem> applications = List.of(app1);
        Page<ApplicationListItem> pagedApplications = new Page(applications, 2, 1, 7);
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any())).thenReturn(
            pagedApplications
        );

        final Response response = envTarget("/_paged").queryParam("page", 3).queryParam("size", 3).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var responseContent = response.readEntity(ApplicationListItemPagedResult.class);
        var pagedApplicationsResult = (ApplicationListItemPagedResult) responseContent;
        assertEquals(3, pagedApplicationsResult.getPage().getCurrent());
        assertEquals(3, pagedApplicationsResult.getPage().getPerPage());
        assertEquals(1, pagedApplicationsResult.getPage().getSize());
        assertEquals(3, pagedApplicationsResult.getPage().getTotalPages());
        assertEquals(7, pagedApplicationsResult.getPage().getTotalElements());
    }
}

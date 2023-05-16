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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.JerseySpringTest;
import io.gravitee.rest.api.management.rest.model.wrapper.ApplicationListItemPagedResult;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
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
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any()))
            .thenReturn(applications);

        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldGetApplicationsPaged() {
        var app1 = new ApplicationListItem();
        app1.setId("app1");
        app1.setUpdatedAt(new Date());
        var app2 = new ApplicationListItem();
        app2.setId("app2");
        app2.setUpdatedAt(new Date());
        var app3 = new ApplicationListItem();
        app3.setId("app3");
        app3.setUpdatedAt(new Date());

        List<ApplicationListItem> applications = List.of(app1, app2, app3);
        Page<ApplicationListItem> pagedApplications = new Page(applications, 0, 3, 3);
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any()))
            .thenReturn(pagedApplications);

        final Response response = envTarget("/_paged").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var responseContent = response.readEntity(ApplicationListItemPagedResult.class);
        var pagedApplicationsResult = (ApplicationListItemPagedResult) responseContent;
        assertEquals(0, pagedApplicationsResult.getPage().getCurrent());
        assertEquals(20, pagedApplicationsResult.getPage().getPerPage());
        assertEquals(3, pagedApplicationsResult.getPage().getSize());
        assertEquals(1, pagedApplicationsResult.getPage().getTotalPages());
        assertEquals(3, pagedApplicationsResult.getPage().getTotalElements());
    }

    @Test
    public void shouldGetApplicationsPagedLastPage() {
        var app1 = new ApplicationListItem();
        app1.setId("app1");
        app1.setUpdatedAt(new Date());

        List<ApplicationListItem> applications = List.of(app1);
        Page<ApplicationListItem> pagedApplications = new Page(applications, 2, 1, 7);
        when(applicationService.search(eq(GraviteeContext.getExecutionContext()), any(ApplicationQuery.class), any(), any()))
            .thenReturn(pagedApplications);

        final Response response = envTarget("/_paged").queryParam("page", 3).queryParam("size", 3).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var responseContent = response.readEntity(ApplicationListItemPagedResult.class);
        var pagedApplicationsResult = (ApplicationListItemPagedResult) responseContent;
        assertEquals(2, pagedApplicationsResult.getPage().getCurrent());
        assertEquals(3, pagedApplicationsResult.getPage().getPerPage());
        assertEquals(1, pagedApplicationsResult.getPage().getSize());
        assertEquals(3, pagedApplicationsResult.getPage().getTotalPages());
        assertEquals(7, pagedApplicationsResult.getPage().getTotalElements());
    }
}

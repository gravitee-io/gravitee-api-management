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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications";
    }

    private static final String APPLICATION = "my-application";

    @Before
    public void init() {
        resetAllMocks();

        ApplicationListItem applicationListItem1 = new ApplicationListItem();
        applicationListItem1.setId("A");
        applicationListItem1.setName("A");

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");
        applicationListItem2.setName("B");

        Set<ApplicationListItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2));
        doReturn(mockApplications).when(applicationService).findByUser(any());

        doReturn(new Application().id("A").name("A")).when(applicationMapper).convert(eq(applicationListItem1), any());
        doReturn(new Application().id("B").name("B")).when(applicationMapper).convert(eq(applicationListItem2), any());


        ApplicationEntity createdEntity = new ApplicationEntity();
        doReturn(createdEntity).when(applicationService).create(any(),  any());
        doReturn(new Application().id("NEW")).when(applicationMapper).convert(eq(createdEntity), any());

    }

    @Test
    public void shouldGetApplications() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findByUser(any());

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(applicationMapper, Mockito.times(2)).computeApplicationLinks(ac.capture(), eq(null));

        String expectedBasePath = target().getUri().toString();
        List<String> bastPathList = ac.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath+"/A"));
        assertTrue(bastPathList.contains(expectedBasePath+"/B"));

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(2, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    public void shouldGetApplicationsOrderByName() {
        ApplicationListItem applicationListItem1 = new ApplicationListItem();
        applicationListItem1.setId("A");
        applicationListItem1.setName("A");

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");
        applicationListItem2.setName("b");

        ApplicationListItem applicationListItem3 = new ApplicationListItem();
        applicationListItem3.setId("C");
        applicationListItem3.setName("C");

        ApplicationListItem applicationListItem4 = new ApplicationListItem();
        applicationListItem4.setId("D");
        applicationListItem4.setName("d");

        Set<ApplicationListItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2, applicationListItem3, applicationListItem4));
        doReturn(mockApplications).when(applicationService).findByUser(any());

        doReturn(new Application().id("A").name("A")).when(applicationMapper).convert(eq(applicationListItem1), any());
        doReturn(new Application().id("B").name("b")).when(applicationMapper).convert(eq(applicationListItem2), any());
        doReturn(new Application().id("C").name("C")).when(applicationMapper).convert(eq(applicationListItem3), any());
        doReturn(new Application().id("D").name("d")).when(applicationMapper).convert(eq(applicationListItem4), any());


        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(4, applicationsResponse.getData().size());
        assertEquals("A", applicationsResponse.getData().get(0).getId());
        assertEquals("B", applicationsResponse.getData().get(1).getId());
        assertEquals("C", applicationsResponse.getData().get(2).getId());
        assertEquals("D", applicationsResponse.getData().get(3).getId());
    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsDesc() {
        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        SubscriptionEntity subB2 = new SubscriptionEntity();
        subB2.setApplication("B");
        SubscriptionEntity subB3 = new SubscriptionEntity();
        subB3.setApplication("B");
        doReturn(Arrays.asList(subA1, subA2, subB1, subB2, subB3)).when(subscriptionService).search(any());

        final Response response = target().queryParam("order", "-nbSubscriptions").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals("B", applicationsResponse.getData().get(0).getId());
        assertEquals("A", applicationsResponse.getData().get(1).getId());

        Map<String, Object> subscriptionsMetadata = applicationsResponse.getMetadata().get("subscriptions");
        assertEquals(3, subscriptionsMetadata.get("B"));
        assertEquals(2, subscriptionsMetadata.get("A"));
    }

    @Test
    public void shouldGetApplicationsOrderByNbSubscriptionsDescAndName() {
        ApplicationListItem applicationListItem1 = new ApplicationListItem();
        applicationListItem1.setId("A");
        applicationListItem1.setName("A");

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");
        applicationListItem2.setName("b");

        ApplicationListItem applicationListItem3 = new ApplicationListItem();
        applicationListItem3.setId("C");
        applicationListItem3.setName("C");

        ApplicationListItem applicationListItem4 = new ApplicationListItem();
        applicationListItem4.setId("D");
        applicationListItem4.setName("d");

        Set<ApplicationListItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2, applicationListItem3, applicationListItem4));
        doReturn(mockApplications).when(applicationService).findByUser(any());

        doReturn(new Application().id("A").name("A")).when(applicationMapper).convert(eq(applicationListItem1), any());
        doReturn(new Application().id("B").name("b")).when(applicationMapper).convert(eq(applicationListItem2), any());
        doReturn(new Application().id("C").name("C")).when(applicationMapper).convert(eq(applicationListItem3), any());
        doReturn(new Application().id("D").name("d")).when(applicationMapper).convert(eq(applicationListItem4), any());

        SubscriptionEntity subA1 = new SubscriptionEntity();
        subA1.setApplication("A");
        SubscriptionEntity subA2 = new SubscriptionEntity();
        subA2.setApplication("A");
        SubscriptionEntity subB1 = new SubscriptionEntity();
        subB1.setApplication("B");
        SubscriptionEntity subB2 = new SubscriptionEntity();
        subB2.setApplication("B");
        SubscriptionEntity subB3 = new SubscriptionEntity();
        subB3.setApplication("B");
        SubscriptionEntity subC1 = new SubscriptionEntity();
        subC1.setApplication("C");
        SubscriptionEntity subC2 = new SubscriptionEntity();
        subC2.setApplication("C");
        SubscriptionEntity subC3 = new SubscriptionEntity();
        subC3.setApplication("C");
        SubscriptionEntity subD1 = new SubscriptionEntity();
        subD1.setApplication("D");
        SubscriptionEntity subD2 = new SubscriptionEntity();
        subD2.setApplication("D");
        doReturn(Arrays.asList(subA1, subA2, subB1, subB2, subB3, subC1, subC2, subC3, subD1, subD2)).when(subscriptionService).search(any());

        final Response response = target().queryParam("order", "-nbSubscriptions").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals("B", applicationsResponse.getData().get(0).getId());
        assertEquals("C", applicationsResponse.getData().get(1).getId());
        assertEquals("A", applicationsResponse.getData().get(2).getId());
        assertEquals("D", applicationsResponse.getData().get(3).getId());

    }

    @Test
    public void shouldGetApplicationsWithPaginatedLink() {
        final Response response = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(1, applicationsResponse.getData().size());
        assertEquals("B", applicationsResponse.getData().get(0).getId());

        Links links = applicationsResponse.getLinks();
        assertNotNull(links);

    }

    @Test
    public void shouldGetNoApplication() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoApplicationAndNoLink() {

        doReturn(new HashSet<>()).when(applicationService).findByUser(any());

        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ApplicationsResponse applicationsResponse = response.readEntity(ApplicationsResponse.class);
        assertEquals(0, applicationsResponse.getData().size());

        Links links = applicationsResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        applicationsResponse = anotherResponse.readEntity(ApplicationsResponse.class);
        assertEquals(0, applicationsResponse.getData().size());

        links = applicationsResponse.getLinks();
        assertNull(links);

    }

    @Test
    public void shouldCreateApplicationWithoutSettings() {

        ApplicationInput input = new ApplicationInput()
                .description(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .name(APPLICATION)
                ;

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getoAuthClient());


        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithEmptySettings() {

        ApplicationInput input = new ApplicationInput()
                .description(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .name(APPLICATION)
                .settings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings())
                ;

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getoAuthClient());


        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithSimpleSettings() {

        ApplicationInput input = new ApplicationInput()
                .description(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .name(APPLICATION)
                ;

        SimpleApplicationSettings sas = new SimpleApplicationSettings()
                .clientId(APPLICATION)
                .type(APPLICATION);
        input.setSettings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings().app(sas));

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        final io.gravitee.rest.api.model.application.SimpleApplicationSettings app = settings.getApp();
        assertNotNull(app);
        assertEquals(APPLICATION, app.getClientId());
        assertEquals(APPLICATION, app.getType());
        assertNull(settings.getoAuthClient());

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldCreateApplicationWithOauthSettings() {

        ApplicationInput input = new ApplicationInput()
                .description(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .name(APPLICATION)
                ;

        OAuthClientSettings oacs = new OAuthClientSettings()
                .applicationType(APPLICATION)
                .clientId(APPLICATION)
                .clientSecret(APPLICATION)
                .clientUri(APPLICATION)
                .logoUri(APPLICATION)
                .grantTypes(Arrays.asList(APPLICATION))
                .redirectUris(Arrays.asList(APPLICATION))
                .responseTypes(Arrays.asList(APPLICATION))
                .renewClientSecretSupported(Boolean.TRUE)
                ;
        input.setSettings(new io.gravitee.rest.api.portal.rest.model.ApplicationSettings().oauth(oacs));

        final Response response = target().request().post(Entity.json(input));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());

        ArgumentCaptor<NewApplicationEntity> captor = ArgumentCaptor.forClass(NewApplicationEntity.class);
        Mockito.verify(applicationService).create(captor.capture(), any());

        final NewApplicationEntity value = captor.getValue();
        assertNotNull(value);
        assertEquals(APPLICATION, value.getDescription());
        assertEquals(APPLICATION, value.getName());
        final Set<String> groups = value.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));

        assertNull(value.getType());
        assertNull(value.getClientId());

        final ApplicationSettings settings = value.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        final io.gravitee.rest.api.model.application.OAuthClientSettings oauthClientSettings = settings.getoAuthClient();
        assertNotNull(oauthClientSettings);
        assertEquals(APPLICATION, oauthClientSettings.getApplicationType());
        final List<String> grantTypes = oauthClientSettings.getGrantTypes();
        assertNotNull(grantTypes);
        assertFalse(grantTypes.isEmpty());
        assertEquals(APPLICATION, grantTypes.get(0));

        final List<String> redirectUris = oauthClientSettings.getRedirectUris();
        assertNotNull(redirectUris);
        assertFalse(redirectUris.isEmpty());
        assertEquals(APPLICATION, redirectUris.get(0));

        Application createdApp = response.readEntity(Application.class);
        assertNotNull(createdApp);
        assertEquals("NEW", createdApp.getId());
    }

    @Test
    public void shouldHaveBadRequestWhileCreatingApplication() {
        final Response response = target().request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}

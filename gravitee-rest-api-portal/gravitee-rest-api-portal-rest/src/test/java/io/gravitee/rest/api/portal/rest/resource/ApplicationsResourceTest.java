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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationInput;
import io.gravitee.rest.api.portal.rest.model.ApplicationsResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.OAuthClientSettings;
import io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings;

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

        ApplicationListItem applicationListItem2 = new ApplicationListItem();
        applicationListItem2.setId("B");

        Set<ApplicationListItem> mockApplications = new HashSet<>(Arrays.asList(applicationListItem1, applicationListItem2));
        doReturn(mockApplications).when(applicationService).findByUser(any());

        doReturn(new Application().id("A")).when(applicationMapper).convert(applicationListItem1);
        doReturn(new Application().id("B")).when(applicationMapper).convert(applicationListItem2);


        ApplicationEntity createdEntity = new ApplicationEntity();
        doReturn(createdEntity).when(applicationService).create(any(),  any());
        doReturn(new Application().id("NEW")).when(applicationMapper).convert(createdEntity);

    }

    @Test
    public void shouldGetApplications() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findByUser(any());

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(applicationMapper, Mockito.times(2)).computeApplicationLinks(ac.capture());

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
        assertEquals("400", error.getCode());
        assertEquals("javax.ws.rs.BadRequestException", error.getTitle());
        assertEquals("page is not valid", error.getDetail());
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
        assertEquals(APPLICATION, oauthClientSettings.getClientId());
        assertEquals(APPLICATION, oauthClientSettings.getClientSecret());
        assertEquals(APPLICATION, oauthClientSettings.getClientUri());
        assertEquals(APPLICATION, oauthClientSettings.getLogoUri());

        final List<String> grantTypes = oauthClientSettings.getGrantTypes();
        assertNotNull(grantTypes);
        assertFalse(grantTypes.isEmpty());
        assertEquals(APPLICATION, grantTypes.get(0));

        final List<String> redirectUris = oauthClientSettings.getRedirectUris();
        assertNotNull(redirectUris);
        assertFalse(redirectUris.isEmpty());
        assertEquals(APPLICATION, redirectUris.get(0));

        final List<String> responseTypes = oauthClientSettings.getResponseTypes();
        assertNotNull(responseTypes);
        assertFalse(responseTypes.isEmpty());
        assertEquals(APPLICATION, responseTypes.get(0));

        assertTrue(oauthClientSettings.isRenewClientSecretSupported());


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
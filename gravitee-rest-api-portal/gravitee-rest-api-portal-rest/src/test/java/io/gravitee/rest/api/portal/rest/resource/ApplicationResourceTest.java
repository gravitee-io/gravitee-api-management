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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
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
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.OAuthClientSettings;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }
    
    private static final String APPLICATION = "my-application";
    private static final String UNKNOWN_APPLICATION = "my-unknown-application";

    @Before
    public void init() {
        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION);
        
        reset(applicationService);
        reset(applicationMapper);
        
        doReturn(applicationEntity).when(applicationService).findById(APPLICATION);
        doReturn(new Application().id(APPLICATION)).when(applicationMapper).convert(applicationEntity);
    }
    
    @Test
    public void shouldDeleteApplication() {
        doNothing().when(applicationService).archive(APPLICATION);

        final Response response = target(APPLICATION).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        Mockito.verify(applicationService).archive(APPLICATION);
    }
    
    @Test
    public void shouldHaveNotFoundWhileDeletingApplication() {
        doThrow(ApplicationNotFoundException.class).when(applicationService).archive(UNKNOWN_APPLICATION);
        
        final Response response = target(UNKNOWN_APPLICATION).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileGettingApplication() {
        doThrow(ApplicationNotFoundException.class).when(applicationService).findById(UNKNOWN_APPLICATION);
        
        final Response response = target(UNKNOWN_APPLICATION).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileUpdatingApplication() {
        doThrow(ApplicationNotFoundException.class).when(applicationService).findById(UNKNOWN_APPLICATION);
        
        final Response response = target(UNKNOWN_APPLICATION).request().put(Entity.json(new Application().id(UNKNOWN_APPLICATION)));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveBadRequestWhileUpdatingApplication() {
        final Response response = target(APPLICATION).request().put(Entity.json(new Application().id(UNKNOWN_APPLICATION)));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        assertEquals("'applicationId' is not the same that the application in payload", response.readEntity(String.class));
    }
    
    @Test
    public void shouldHaveForbiddenWhileUpdatingApplication() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId("my-user");
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(APPLICATION);
        
        final Response response = target(APPLICATION).request().put(Entity.json(new Application().id(APPLICATION)));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
    
    @Test
    public void shouldUpdateApplicationWithoutSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(APPLICATION);
        
        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION);
        doReturn(updatedEntity).when(applicationService).update(eq(APPLICATION), any());

        Application appInput = new Application()
                .id(APPLICATION)
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                ;
        final Response response = target(APPLICATION).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION, updateAppEntity.getName());
        assertEquals(APPLICATION, updateAppEntity.getDescription());
        final Set<String> groups = updateAppEntity.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));
        assertNull(updateAppEntity.getSettings());
        
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
    }
    
    @Test
    public void shouldUpdateApplicationWithEmptySettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(APPLICATION);
        
        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION);
        doReturn(updatedEntity).when(applicationService).update(eq(APPLICATION), any());

        Application appInput = new Application()
                .id(APPLICATION)
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .settings(new ApplicationSettings())
                ;
        final Response response = target(APPLICATION).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION, updateAppEntity.getName());
        assertEquals(APPLICATION, updateAppEntity.getDescription());
        final Set<String> groups = updateAppEntity.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getoAuthClient());
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
    }
    
    @Test
    public void shouldUpdateApplicationWithSimpleAppSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(APPLICATION);
        
        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION);
        doReturn(updatedEntity).when(applicationService).update(eq(APPLICATION), any());

        Application appInput = new Application()
                .id(APPLICATION)
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .settings(
                        new ApplicationSettings()
                            .app(
                                    new SimpleApplicationSettings()
                                        .clientId(APPLICATION)
                                        .type(APPLICATION)
                            )
                )
                ;
        final Response response = target(APPLICATION).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION, updateAppEntity.getName());
        assertEquals(APPLICATION, updateAppEntity.getDescription());
        final Set<String> groups = updateAppEntity.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        final io.gravitee.rest.api.model.application.SimpleApplicationSettings app = settings.getApp();
        assertNotNull(app);
        assertEquals(APPLICATION, app.getClientId());
        assertEquals(APPLICATION, app.getType());
        assertNull(settings.getoAuthClient());
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
    }
    
    @Test
    public void shouldUpdateApplicationWithOAuthClientSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(APPLICATION);
        
        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION);
        doReturn(updatedEntity).when(applicationService).update(eq(APPLICATION), any());

        Application appInput = new Application()
                .id(APPLICATION)
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .settings(
                        new ApplicationSettings()
                            .oauth(
                                    new OAuthClientSettings()
                                        .applicationType(APPLICATION)
                                        .clientId(APPLICATION)
                                        .clientSecret(APPLICATION)
                                        .clientUri(APPLICATION)
                                        .logoUri(APPLICATION)
                                        .grantTypes(Arrays.asList(APPLICATION))
                                        .redirectUris(Arrays.asList(APPLICATION))
                                        .responseTypes(Arrays.asList(APPLICATION))
                                        .renewClientSecretSupported(Boolean.TRUE)
                            )
                )
                ;
        final Response response = target(APPLICATION).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION, updateAppEntity.getName());
        assertEquals(APPLICATION, updateAppEntity.getDescription());
        final Set<String> groups = updateAppEntity.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        final io.gravitee.rest.api.model.application.OAuthClientSettings oAuthClientSettings = settings.getoAuthClient();
        assertNotNull(oAuthClientSettings);
        assertEquals(APPLICATION, oAuthClientSettings.getApplicationType());
        assertEquals(APPLICATION, oAuthClientSettings.getClientId());
        assertEquals(APPLICATION, oAuthClientSettings.getClientSecret());
        assertEquals(APPLICATION, oAuthClientSettings.getClientUri());
        assertEquals(APPLICATION, oAuthClientSettings.getLogoUri());
        
        final List<String> grantTypes = oAuthClientSettings.getGrantTypes();
        assertNotNull(grantTypes);
        assertFalse(grantTypes.isEmpty());
        assertEquals(APPLICATION, grantTypes.get(0));
        
        final List<String> redirectUris = oAuthClientSettings.getRedirectUris();
        assertNotNull(redirectUris);
        assertFalse(redirectUris.isEmpty());
        assertEquals(APPLICATION, redirectUris.get(0));
        
        final List<String> responseTypes = oAuthClientSettings.getResponseTypes();
        assertNotNull(responseTypes);
        assertFalse(responseTypes.isEmpty());
        assertEquals(APPLICATION, responseTypes.get(0));
        
        assertTrue(oAuthClientSettings.isRenewClientSecretSupported());
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
    }
    
    @Test
    public void shouldGetApplicationWithoutInclude() {
        final Response response = target(APPLICATION).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
    }
    
    @Test
    public void shouldGetApplicationWithPlans() {
        SubscriptionEntity sub1 = new SubscriptionEntity();
        sub1.setPlan("A");
        SubscriptionEntity sub2 = new SubscriptionEntity();
        sub2.setPlan("B");
        doReturn(Arrays.asList(sub1, sub2)).when(subscriptionService).findByApplicationAndPlan(APPLICATION, null);
        
        PlanEntity pEnt1 = new PlanEntity();
        pEnt1.setId("A");
        PlanEntity pEnt2 = new PlanEntity();
        pEnt2.setId("B");
        doReturn(pEnt1).when(planService).findById("A");
        doReturn(pEnt2).when(planService).findById("B");

        Plan p1 = new Plan().id("A");
        Plan p2 = new Plan().id("B");
        
        List<Plan> expectedPlans = Arrays.asList(p1, p2);
        doReturn(p1).when(planMapper).convert(pEnt1);
        doReturn(p2).when(planMapper).convert(pEnt2);
        
        final Response response = target(APPLICATION).queryParam("include","plans").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        final List<Plan> plans = applicationResponse.getPlans();
        assertNotNull(plans);
        assertFalse(plans.isEmpty());
        assertTrue(plans.containsAll(expectedPlans));
        
    }
    
    @Test
    public void shouldRenewApplication() {
        ApplicationEntity renewedApplicationEntity = new ApplicationEntity();
        renewedApplicationEntity.setId(APPLICATION);
        doReturn(renewedApplicationEntity).when(applicationService).renewClientSecret(APPLICATION);

        final Response response = target(APPLICATION).path("_renew_secret").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).renewClientSecret(APPLICATION);
        Mockito.verify(applicationMapper).convert(renewedApplicationEntity);

        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertNotNull(applicationResponse);
    }
    
}

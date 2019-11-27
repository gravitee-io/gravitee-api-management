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
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import org.apache.http.client.utils.DateUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    private InlinePictureEntity mockImage;
    private byte[] applicationLogoContent;
    
    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();
        
        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION);
        
        doReturn(applicationEntity).when(applicationService).findById(APPLICATION);
        doReturn(new Application().id(APPLICATION)).when(applicationMapper).convert(applicationEntity);
        
        mockImage = new InlinePictureEntity();
        applicationLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(applicationLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(applicationService).getPicture(APPLICATION);
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
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals("'applicationId' is not the same that the application in payload", errorResponse.getErrors().get(0).getMessage());
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

        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(updatedEntity);
        
        String newPicture = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
        String scaledPicture = "data:image/gif;base64,R0lGODlhyADIAPAAAAAAAP///ywAAAAAyADIAEAC/4SPqcvtD6OctNqL"
                + "s968+w+G4kiW5omm6sq27gvH8kzX9o3n+s73/g8MCofEovGITCqXzKbzCY1Kp9Sq9YrNarfcrvcLDovH5LL5jE6r1+y2+w2Py+"
                + "f0uv2Oz+v3/L7/DxgoOEhYaHiImKi4yNjo+AgZKTlJWWl5iZmpucnZ6fkJGio6SlpqeoqaqrrK2ur6ChsrO0tba3uLm6u7y9vr"
                + "+wscLDxMXGx8jJysvMzc7PwMHS09TV1tfY2drb3N3e39DR4uPk5ebn6Onq6+zt7u/g4fLz9PX29/j5+vv8/f7/8PMKDAgQQLGj"
                + "yIMKHChQwbOnwIMaLEiRQrWryIMaPGjQYcO3osUwAAOw==";
        Application appInput = new Application()
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .picture(newPicture)
                ;
        appInput.setId(APPLICATION);
        
        final Response response = target(APPLICATION).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION, updateAppEntity.getName());
        assertEquals(APPLICATION, updateAppEntity.getDescription());
        assertEquals(scaledPicture, updateAppEntity.getPicture());
        final Set<String> groups = updateAppEntity.getGroups();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertTrue(groups.contains(APPLICATION));
        assertNull(updateAppEntity.getSettings());
        
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
        final MultivaluedMap<String, Object> headers = response.getHeaders();
        String lastModified = (String) headers.getFirst(HttpHeader.LAST_MODIFIED.asString());
        String etag = (String) headers.getFirst("ETag");
        
        assertEquals(nowDate.toInstant().getEpochSecond(), DateUtils.parseDate(lastModified).toInstant().getEpochSecond());
        
        String expectedTag = '"'+Long.toString(nowDate.getTime())+'"';
        assertEquals(expectedTag, etag);

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

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(updatedEntity);
        
        Application appInput = new Application()
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
                .settings(new ApplicationSettings())
                ;
        appInput.setId(APPLICATION);
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

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(updatedEntity);
        
        Application appInput = new Application()
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
        appInput.setId(APPLICATION);
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

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(updatedEntity);
        
        
        Application appInput = new Application()
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
        appInput.setId(APPLICATION);
        
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
    public void shouldGetApplication() {
        final Response response = target(APPLICATION).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(applicationService).findById(APPLICATION);
        
        String expectedBasePath = target(APPLICATION).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath);
        
        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION, applicationResponse.getId());
        
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
    
    @Test
    public void shouldGetApplicationPicture() throws IOException {
        final Response response = target(APPLICATION).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

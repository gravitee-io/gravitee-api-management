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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.http.client.utils.DateUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }

    private static final String APPLICATION_ID = "my-application-id";
    private static final String APPLICATION_PICTURE = "my-application-picture";
    private static final String UNKNOWN_APPLICATION_ID = "my-unknown-application-id";

    private InlinePictureEntity mockImage;
    private byte[] applicationLogoContent;

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION_ID);

        doReturn(applicationEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        doReturn(new Application().id(APPLICATION_ID))
            .when(applicationMapper)
            .convert(eq(GraviteeContext.getExecutionContext()), eq(applicationEntity), any());

        mockImage = new InlinePictureEntity();
        applicationLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(applicationLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(applicationService).getPicture(GraviteeContext.getExecutionContext(), APPLICATION_PICTURE);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldDeleteApplication() {
        doNothing().when(applicationService).archive(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        final Response response = target(APPLICATION_ID).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        Mockito.verify(applicationService).archive(GraviteeContext.getExecutionContext(), APPLICATION_ID);
    }

    @Test
    public void shouldHaveNotFoundWhileDeletingApplication() {
        doThrow(ApplicationNotFoundException.class)
            .when(applicationService)
            .archive(GraviteeContext.getExecutionContext(), UNKNOWN_APPLICATION_ID);

        final Response response = target(UNKNOWN_APPLICATION_ID).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileGettingApplication() {
        doThrow(ApplicationNotFoundException.class)
            .when(applicationService)
            .findById(GraviteeContext.getExecutionContext(), UNKNOWN_APPLICATION_ID);

        final Response response = target(UNKNOWN_APPLICATION_ID).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveNotFoundWhileUpdatingApplication() {
        doThrow(ApplicationNotFoundException.class)
            .when(applicationService)
            .findById(GraviteeContext.getExecutionContext(), UNKNOWN_APPLICATION_ID);

        final Response response = target(UNKNOWN_APPLICATION_ID).request().put(Entity.json(new Application().id(UNKNOWN_APPLICATION_ID)));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldHaveBadRequestWhileUpdatingApplication() {
        final Response response = target(APPLICATION_ID).request().put(Entity.json(new Application().id(UNKNOWN_APPLICATION_ID)));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        assertEquals("'applicationId' is not the same that the application in payload", errorResponse.getErrors().get(0).getMessage());
    }

    @Test
    public void shouldUpdateApplicationWithoutSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION_ID);
        doReturn(updatedEntity).when(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION_ID);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(updatedEntity), any());

        String newPicture = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
        String scaledPicture =
            "data:image/gif;base64,R0lGODlhyADIAPAAAAAAAP///ywAAAAAyADIAEAC/4SPqcvtD6OctNqL" +
            "s968+w+G4kiW5omm6sq27gvH8kzX9o3n+s73/g8MCofEovGITCqXzKbzCY1Kp9Sq9YrNarfcrvcLDovH5LL5jE6r1+y2+w2Py+" +
            "f0uv2Oz+v3/L7/DxgoOEhYaHiImKi4yNjo+AgZKTlJWWl5iZmpucnZ6fkJGio6SlpqeoqaqrrK2ur6ChsrO0tba3uLm6u7y9vr" +
            "+wscLDxMXGx8jJysvMzc7PwMHS09TV1tfY2drb3N3e39DR4uPk5ebn6Onq6+zt7u/g4fLz9PX29/j5+vv8/f7/8PMKDAgQQLGj" +
            "yIMKHChQwbOnwIMaLEiRQrWryIMaPGjQYcO3osUwAAOw==";
        Application appInput = new Application().description(APPLICATION_ID).name(APPLICATION_ID).picture(newPicture);
        appInput.setId(APPLICATION_ID);

        final Response response = target(APPLICATION_ID).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION_ID, updateAppEntity.getName());
        assertEquals(APPLICATION_ID, updateAppEntity.getDescription());
        assertEquals(scaledPicture, updateAppEntity.getPicture());
        assertNull(updateAppEntity.getSettings());

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, updatedApp.getUpdatedAt());

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());

        final MultivaluedMap<String, Object> headers = response.getHeaders();
        String lastModified = (String) headers.getFirst(HttpHeader.LAST_MODIFIED.asString());
        String etag = (String) headers.getFirst("ETag");

        assertEquals(nowDate.toInstant().getEpochSecond(), DateUtils.parseDate(lastModified).toInstant().getEpochSecond());

        String expectedTag = '"' + Long.toString(nowDate.getTime()) + '"';
        assertEquals(expectedTag, etag);
    }

    @Test
    public void shouldUpdateApplicationWithEmptySettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION_ID);
        doReturn(updatedEntity).when(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION_ID);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(updatedEntity), any());

        Application appInput = new Application().description(APPLICATION_ID).name(APPLICATION_ID).settings(new ApplicationSettings());
        appInput.setId(APPLICATION_ID);
        final Response response = target(APPLICATION_ID).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION_ID, updateAppEntity.getName());
        assertEquals(APPLICATION_ID, updateAppEntity.getDescription());
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        assertNull(settings.getOAuthClient());

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, updatedApp.getUpdatedAt());

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());
    }

    @Test
    public void shouldUpdateApplicationWithSimpleAppSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        io.gravitee.rest.api.model.application.ApplicationSettings appSettings =
            new io.gravitee.rest.api.model.application.ApplicationSettings();
        io.gravitee.rest.api.model.application.SimpleApplicationSettings simpleApp =
            new io.gravitee.rest.api.model.application.SimpleApplicationSettings();
        appSettings.setApp(simpleApp);
        appEntity.setSettings(appSettings);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION_ID);
        doReturn(updatedEntity).when(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION_ID);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(updatedEntity), any());

        Application appInput = new Application()
            .description(APPLICATION_ID)
            .name(APPLICATION_ID)
            .settings(new ApplicationSettings().app(new SimpleApplicationSettings().clientId(APPLICATION_ID).type(APPLICATION_ID)));
        appInput.setId(APPLICATION_ID);
        final Response response = target(APPLICATION_ID).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION_ID, updateAppEntity.getName());
        assertEquals(APPLICATION_ID, updateAppEntity.getDescription());
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        final io.gravitee.rest.api.model.application.SimpleApplicationSettings app = settings.getApp();
        assertNotNull(app);
        assertEquals(APPLICATION_ID, app.getClientId());
        assertEquals(APPLICATION_ID, app.getType());
        assertNull(settings.getOAuthClient());

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, updatedApp.getUpdatedAt());

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());
    }

    @Test
    public void shouldUpdateApplicationWithTlsSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        io.gravitee.rest.api.model.application.ApplicationSettings appSettings =
            new io.gravitee.rest.api.model.application.ApplicationSettings();

        appSettings.setTls(TlsSettings.builder().clientCertificate("certificate").build());
        appEntity.setSettings(appSettings);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION_ID);
        doReturn(updatedEntity).when(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION_ID);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(updatedEntity), any());

        Application appInput = new Application()
            .description(APPLICATION_ID)
            .name(APPLICATION_ID)
            .settings(new ApplicationSettings().tls(new TlsClientSettings().clientCertificate("certificate_updated")));
        appInput.setId(APPLICATION_ID);
        final Response response = target(APPLICATION_ID).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION_ID, updateAppEntity.getName());
        assertEquals(APPLICATION_ID, updateAppEntity.getDescription());
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        final TlsSettings tlsResult = settings.getTls();
        assertNotNull(tlsResult);
        assertEquals("certificate_updated", tlsResult.getClientCertificate());
        assertNull(settings.getOAuthClient());
        assertNull(settings.getApp());

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, updatedApp.getUpdatedAt());

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());
    }

    @Test
    public void shouldUpdateApplicationWithOAuthClientSettings() {
        ApplicationEntity appEntity = new ApplicationEntity();
        io.gravitee.rest.api.model.application.ApplicationSettings appSettings =
            new io.gravitee.rest.api.model.application.ApplicationSettings();
        io.gravitee.rest.api.model.application.OAuthClientSettings oauthClientSettings =
            new io.gravitee.rest.api.model.application.OAuthClientSettings();
        appSettings.setOAuthClient(oauthClientSettings);
        appEntity.setSettings(appSettings);
        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_NAME);
        PrimaryOwnerEntity owner = new PrimaryOwnerEntity(userEntity);
        appEntity.setPrimaryOwner(owner);
        doReturn(appEntity).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ApplicationEntity updatedEntity = new ApplicationEntity();
        updatedEntity.setId(APPLICATION_ID);
        doReturn(updatedEntity).when(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), any());

        Instant now = Instant.now();
        Application updatedApp = new Application();
        updatedApp.setId(APPLICATION_ID);
        updatedApp.setUpdatedAt(now.atOffset(ZoneOffset.UTC));
        doReturn(updatedApp).when(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(updatedEntity), any());

        Application appInput = new Application()
            .description(APPLICATION_ID)
            .name(APPLICATION_ID)
            .settings(
                new ApplicationSettings()
                    .oauth(
                        new OAuthClientSettings()
                            .applicationType(APPLICATION_ID)
                            .clientId(APPLICATION_ID)
                            .clientSecret(APPLICATION_ID)
                            .clientUri(APPLICATION_ID)
                            .logoUri(APPLICATION_ID)
                            .grantTypes(Arrays.asList(APPLICATION_ID))
                            .redirectUris(Arrays.asList(APPLICATION_ID))
                            .responseTypes(Arrays.asList(APPLICATION_ID))
                            .renewClientSecretSupported(Boolean.TRUE)
                    )
            );
        appInput.setId(APPLICATION_ID);

        final Response response = target(APPLICATION_ID).request().put(Entity.json(appInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        ArgumentCaptor<UpdateApplicationEntity> captor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID), captor.capture());
        UpdateApplicationEntity updateAppEntity = captor.getValue();
        assertEquals(APPLICATION_ID, updateAppEntity.getName());
        assertEquals(APPLICATION_ID, updateAppEntity.getDescription());
        final io.gravitee.rest.api.model.application.ApplicationSettings settings = updateAppEntity.getSettings();
        assertNotNull(settings);
        assertNull(settings.getApp());
        final io.gravitee.rest.api.model.application.OAuthClientSettings oAuthClientSettings = settings.getOAuthClient();
        assertNotNull(oAuthClientSettings);
        final List<String> grantTypes = oAuthClientSettings.getGrantTypes();
        assertNotNull(grantTypes);
        assertFalse(grantTypes.isEmpty());
        assertEquals(APPLICATION_ID, grantTypes.get(0));

        final List<String> redirectUris = oAuthClientSettings.getRedirectUris();
        assertNotNull(redirectUris);
        assertFalse(redirectUris.isEmpty());
        assertEquals(APPLICATION_ID, redirectUris.get(0));

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, updatedApp.getUpdatedAt());

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());
    }

    @Test
    public void shouldGetApplication() {
        final Response response = target(APPLICATION_ID).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, null);

        Application applicationResponse = response.readEntity(Application.class);
        assertEquals(APPLICATION_ID, applicationResponse.getId());
    }

    @Test
    public void shouldRenewApplication() {
        ApplicationEntity renewedApplicationEntity = new ApplicationEntity();
        renewedApplicationEntity.setId(APPLICATION_ID);
        doReturn(renewedApplicationEntity)
            .when(applicationService)
            .renewClientSecret(GraviteeContext.getExecutionContext(), APPLICATION_ID);

        final Response response = target(APPLICATION_ID).path("_renew_secret").request().post(null);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(applicationService).renewClientSecret(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        Mockito.verify(applicationMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(renewedApplicationEntity), any());

        String expectedBasePath = target(APPLICATION_ID).getUri().toString();
        Mockito.verify(applicationMapper).computeApplicationLinks(expectedBasePath, null);

        Application applicationResponse = response.readEntity(Application.class);
        assertNotNull(applicationResponse);
    }

    @Test
    public void shouldGetApplicationPicture() throws IOException {
        final Response response = target(APPLICATION_ID).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());
    }
}

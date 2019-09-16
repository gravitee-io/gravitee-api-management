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

import static io.gravitee.common.http.HttpStatusCode.NOT_MODIFIED_304;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.client.utils.DateUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.OAuthClientSettings;
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
                .description(APPLICATION)
                .name(APPLICATION)
                .groups(Arrays.asList(APPLICATION))
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

        MultivaluedMap<String, Object> headers = response.getHeaders();
        String contentType = (String) headers.getFirst(HttpHeader.CONTENT_TYPE.asString());
        String etag = (String) headers.getFirst("ETag");

        assertEquals(mockImage.getType(), contentType);

        File result = response.readEntity(File.class);
        byte[] fileContent = Files.readAllBytes(Paths.get(result.getAbsolutePath()));
        assertTrue(Arrays.equals(fileContent, applicationLogoContent));
        
        String expectedTag = '"'+Integer.toString(new String(fileContent).hashCode())+'"';
        assertEquals(expectedTag, etag);
        
        
        // test Cache
        final Response cachedResponse = target(APPLICATION).path("picture").request().header(HttpHeader.IF_NONE_MATCH.asString(), etag).get();
        assertEquals(NOT_MODIFIED_304, cachedResponse.getStatus());
    }
    
    @Test
    public void shouldUpdateApplicationPicture() throws IOException {
        final String pictureData = "data:image/jpeg;base64,"
                + "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAx"
                + "NDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIy"
                + "MjIyMjIyMjL/wAARCADIAMgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUF"
                + "BAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVW"
                + "V1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi"
                + "4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAEC"
                + "AxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVm"
                + "Z2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq"
                + "8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD3+iiigAooooAKKKKACiiigAopM0ZoAWijNJmgBaKTNLmgAoopM0ALRSZpaACiiigAoooo"
                + "AKKKKACiiigAooooAKKKKACiiigAprHA5P5UprJmkubq9lghk8pI8Bm/AH+tAGg86AdvxqI3Ua9ZIR9WFVRpCsczXFxIfaQgflUh"
                + "0ixzlrdWPqyg0gFbUYB1uYh9OaadXtO92v8A3yaeNOsV6WVufrEv+FI1jZAf8edt/wB+l/woHYiOr2Pe7H/fJpP7Wsu14o/4CaVr"
                + "Kz/587b/AL9LTDY2X/Pnbf8Aflf8KljsTLqtm3S8jP1NSfbYW+7c25/4EP8AGqLaXp7dbC2/CFf8KgfRNNbpbIp9UQKf5UXA2o7l"
                + "D0Kn/dNTq4I4rlJdGSLm2u7yE/8AXZsflUdtfajpl/bx3FwLq3mYoGP3l4J5OT6UKQNHY45zS0wdR1Ip9WSFFFFABRRRQAUUUUAF"
                + "FFFABRRRQAUUUUANJ5xjNZdsSNUviemRjI9hWoeveuN1JZZNbuFS4dQWGVU4z8orDEV1RhzMunDnlY68sFHzED8cVG1xEv8Ay2jH"
                + "1YVSW2QKAS5wP4nNL5EX9wfnmo+se7ew+SxZN5AOs0X/AH0KhkvrRVJN1Fgf7Q/xqI2sJ/5ZimTW0JhZfLQ8HsKwnjVGN7FRgr7k"
                + "f9taYf8Al+g/F1/xpP7Y0w/8v9v/AN/F/wAaw20iFvmEK8+1RnSYh/yxH5V5zzh32OpYeHc6H+17Bul7bf8Af1f8acuoWbf8vlsf"
                + "pKv+Ncq+lRDOIsfhVaTTUA7/AJkVSzhdUP6pF9TsHuInPySxN/20BrJ1UnzbPG0gTE/Lxztrlp7Vk+5JKP8Adc/41FZPcf2xZRNd"
                + "O8YkPysc9jXTh8xjUmo2JqYXli3c9gB5p9NB+bpTq9dHAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUANI5rlEK3GsSSYyC4/lXT3Eg"
                + "ihkkPG1Sa5bTBmYN65P615eZy92MO7N8OtWzez196M5qPd70bveiTtoVYkzTJDlWpN1Nc/K1cNZ+6yktSnwMio2p2etMY18/KWp1"
                + "pEEuMGqE3Srsp4NUZTxWDkaRM25UE1mO4t7uCYcbHBJ+vFadx1rHvBuV1+mPwOa68HNwqxkayV4NHsyEMoYdDT6y/D919r0Szmzk"
                + "vECfritSvvY7HgtWYUUUUxBRRRQAUUUUAFFFFABRRRQAUUUUAUtVO3S7lvSM1z2l/fx6L/Wuh1WMy6ZdRr1aMgVzWkPum56FcfrX"
                + "k5ivfgzrw/wyNnNANNHelqZDHZpHP7s0lDn5GFclb4WNblLNRs1LnJNRsa+ZnLU7IohlbiqMrVblPFUZjxULU1SKNw3NZV0Rtb6V"
                + "oTt1rHu3IUnsOa7aCvJGyVk7npfgd9/hyAH+E7R+Qrpa5zwTA0Phq1LDBcb/AMwK6OvvKfwo+dn8TCiiirJCiiigAooooAKKKKAC"
                + "iiigAooooAYwByp6GuMQGz1J4egRv5812h61yPiNPK1FGH/LRcmuHHU+aF+x0YZ+/buavp70tMibfEj+op+c1zPVXLe9gpsn3WpT"
                + "TXPyH6VyVl7jKjujPLcmonagn5m+tRO1fHzn7zPQSI5TxVKVuDVmVuKoytwaum7msYlCduDWRIrXFzHbqCTKwUY/Or9y3Bqfwhb/"
                + "AGzxTBu5EILfoRXuZdS56qQVnyUmz1Sxtxa2kNuowsS7as00dadX2SVj50KKKKYBRRRQAUUUUAFFFFABRRRQAUUUUANPeuX8UDFx"
                + "Af8AYx+prqD1Nc34pX5bdvfH86wxCvTZrQ0qImsObGD6ZqYjbx1qvpx/4lsJ9v61YNcEfgRtP42NJpj8IfpTjTJD8h+lctf4GVHc"
                + "ymb5m+tQu3NOc4ZvrULtzXws23NnqxWhHK3FUZm4NWpW4NUJ2+U11UUzWKMq6fANa3w7JfxFdH0hGPzrDu24Nb/w0Xdq18/pHj9R"
                + "X0+UR/eowx2lFnqAHzU6kHWlr6k+fCiiigAooooAKKKKACiiigAooooAKKKKAE9a5/xUv+iW59Jh/I10B6GsTxOudOjPpID+hrKt"
                + "/Cl6F0/jRBpR/wCJcPrVpupqnpZ/0Ir3Bq03U15lJ3po6Z/GxpNRSn5D9KexqCX7jewrCv8AAyo7mO7fMfqagd6JHyx69TVZ3r4v"
                + "k99nrxWgSycVnXEny9anmfg1n3D/ACmu6jTNEZt7J8h5rrfhahIv5PU4z/3zXEX0mENd/wDClN2kXco/57lf/HVNfTZTC07nHmLt"
                + "SR6GOpp1IOtLXvnhBRRRQAUUUUAFFFFABRRRQAUUUUAFFFFACGsnxEu7Sn/2Wz+hrWrO1td2j3PHIXIrKqv3bKg7SRlaWf8AR3/3"
                + "v8KtseTVPTCDE4Hrn+VW26mvJw7vSOyfxEbGoJW/dv8ASpWNQOflbP8AdrOt8JUNznJWwzfU1Wd+afK/7xx/tGqsj818tGHvs9dL"
                + "Qjmk4NZ88nymrEz8Hms2d+OtehSgWjLv5PlNep/CuML4Wkf+/OT/AOOrXkl842P+lezfDWLy/B8HH3nLfoK+hyyNrnn5m/3aR2Ge"
                + "aWkFLXrnihRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABVPVV3aXcD1SrlQ3SeZbSJ6ipkrpoFucvoz7kf/AHf61oN+o61i+H5d8skf"
                + "+z/Wtdjkk14eG+B+rO+a1GMarzfdf/dNSseagmb5G+hoq/CXBanJStiWT/eNVZHqadv3sn+8f51Skfmvn4x95nrxWhFM/FZlw/ym"
                + "rk78Gsyd8/nXfTiUZN/JwRmvffAsRi8IWAIxuTdXzxdybmPvxX0p4Zi8nw5YR/3YsV7uAWjPLzN6pGsKWiivSPICiiigAooooAKK"
                + "KKACiiigAooooAKKKKACkIyKWkPSgDz3w/IU1aaLuFroGxXNWSyWfi2RpEaOA8b3GFz9a6Hzon+5IjZ9GBrxqdNw5lbqehJp29Bj"
                + "moJSPLf6GpXqtLu8pzg9DWVTZmkNzj53/fSf7x/nVORualnb9/Lz/Ef51UkbmvGjH3j14tWK87cGsq5l2Ln3q9O/BrIuXypHWvRp"
                + "Qb6A5RXUy5juuI0/vPX1LpcXlaZbxn+FcV8uW9tcXGpQnyZNivy2w4HFfVkS7I1X0Fe3hIcsTxcxmpTVh9FFFdZ5wUUUUAFFFFAB"
                + "RRRQAUUUUAFFFFABRRRQAUjfdNLRQBhtbyWs8u+1+0Qu27hckcf/AFqgk/seT/XRSW/++Sn9a6HYBjFNaGNvvIp+opNBc5v7Bosn"
                + "+qvVX/tsT/WmHRrBtw/tTAI/vf8A166KSwtpeGhX8Bj+VQnR7AnmD/x5v8al04PdFc8ujOPbwppJZidQUknn5v8A69RN4T0T+K9H"
                + "/fX/ANeuy/sLTsk/Zv8Ax9v8aX+w9O/59h/323+NR9Xo/wApftqn8xwE3h/w5FnNxG59GlI/rVM6ZpIbEFm0hPH7vL/1r1BNMso+"
                + "lun4jNTrbwr92JB9FFaRhFbIl1Jvdnm1l4Yu9RlUGyNtAGyWkQoxH0xXpqnKg9aTaKcOBVEavcKKKKACiiigAooooAKKKKACiiig"
                + "AooooAKKKKACiiigAooooAKKKKAEooooCwtJRRQAUUUUALRRRQAUUUUAFFFFABRRRQB//9k=";
        
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        
        ApplicationEntity updatedApplication = new ApplicationEntity();
        updatedApplication.setUpdatedAt(nowDate);
        doReturn(updatedApplication).when(applicationService).update(eq(APPLICATION), any());
        doReturn(new Application().id(APPLICATION)).when(applicationMapper).convert(updatedApplication);

        
        final Response response = target(APPLICATION).path("picture").request().put(Entity.entity(pictureData, MediaType.MULTIPART_FORM_DATA));
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(applicationService).findById(APPLICATION);
        
        ArgumentCaptor<UpdateApplicationEntity> updateAppCaptor = ArgumentCaptor.forClass(UpdateApplicationEntity.class);
        Mockito.verify(applicationService).update(eq(APPLICATION), updateAppCaptor.capture());
        UpdateApplicationEntity updateApp = updateAppCaptor.getValue();
        assertEquals(pictureData, updateApp.getPicture());

        final MultivaluedMap<String, Object> headers = response.getHeaders();
        String lastModified = (String) headers.getFirst(HttpHeader.LAST_MODIFIED.asString());
        String etag = (String) headers.getFirst("ETag");
        
        assertEquals(nowDate.toInstant().getEpochSecond(), DateUtils.parseDate(lastModified).toInstant().getEpochSecond());
        
        String expectedTag = '"'+Long.toString(nowDate.getTime())+'"';
        assertEquals(expectedTag, etag);

        
    }
}

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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationLinks;
import io.gravitee.rest.api.portal.rest.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationMapperTest {

    private static final String APPLICATION = "my-application";
    private static final String APPLICATION_ID = "my-application-id";
    private static final String APPLICATION_DESCRIPTION = "my-application-description";
    private static final String APPLICATION_NAME = "my-application-name";
    private static final String APPLICATION_GROUP = "my-application-group";
    private static final String APPLICATION_STATUS = "my-application-status";
    private static final String APPLICATION_TYPE = "my-application-type";
    private static final String APPLICATION_USER_ID = "my-application-user-id";
    private static final String APPLICATION_SIMPLE_CLIENT_ID = "my-application-simple-client-id";
    private static final String APPLICATION_SIMPLE_TYPE = "my-application-simple-type";
    private static final String APPLICATION_OAUTH_APPLICATION_TYPE = "my-application-oauth-application-type";
    private static final String APPLICATION_OAUTH_CLIENT_ID = "my-application-oauth-client-id";
    private static final String APPLICATION_OAUTH_CLIENT_SECRET = "my-application-oauth-client-secret";
    private static final String APPLICATION_OAUTH_CLIENT_URI = "my-application-oauth--client-uri";
    private static final String APPLICATION_OAUTH_GRANT_TYPE = "my-application-oauth-grant-type";
    private static final String APPLICATION_OAUTH_LOGO_URI = "my-application-oauth-logo-uri";
    private static final String APPLICATION_OAUTH_REDIRECT_URI = "my-application-oauth-redirect-uri";
    private static final String APPLICATION_OAUTH_RESPONSE_TYPE = "my-application-oauth-response-type";

        
    @InjectMocks
    private ApplicationMapper applicationMapper;

    private enum AppSettingsEnum {
        NO_SETTINGS, SIMPLE_SETTINGS, OAUTH_SETTINGS;
    }
    
    @Test
    public void testConvertFromAppListItem() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationListItem applicationListItem;

        //init
        applicationListItem = new ApplicationListItem();
        applicationListItem.setCreatedAt(nowDate);
        applicationListItem.setDescription(APPLICATION_DESCRIPTION);
        applicationListItem.setGroups(new HashSet<String>(Arrays.asList(APPLICATION_GROUP)));
        applicationListItem.setId(APPLICATION_ID);
        applicationListItem.setName(APPLICATION_NAME);
        applicationListItem.setStatus(APPLICATION_STATUS);
        applicationListItem.setType(APPLICATION_TYPE);
        applicationListItem.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION_USER_ID);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationListItem.setPrimaryOwner(primaryOwner);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationListItem);
        checkApplication(now, responseApplication, AppSettingsEnum.NO_SETTINGS);
    }
    
    @Test
    public void testApplicationLinks() {
        String basePath = "/"+APPLICATION;
        
        ApplicationLinks links = applicationMapper.computeApplicationLinks(basePath, null);
        
        assertNotNull(links);
        
        assertEquals(basePath, links.getSelf());
        assertEquals(basePath+"/members", links.getMembers());
        assertEquals(basePath+"/notifications", links.getNotifications());
        assertEquals(basePath+"/picture", links.getPicture());
    }
    
    @Test
    public void testConvertFromAppEntityNoSettings() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationEntity applicationEntity;

        //init
        applicationEntity = new ApplicationEntity();
        applicationEntity.setCreatedAt(nowDate);
        applicationEntity.setDescription(APPLICATION_DESCRIPTION);
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION_GROUP)));
        applicationEntity.setId(APPLICATION_ID);
        applicationEntity.setName(APPLICATION_NAME);
        applicationEntity.setStatus(APPLICATION_STATUS);
        applicationEntity.setType(APPLICATION_TYPE);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION_USER_ID);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationEntity.setPrimaryOwner(primaryOwner);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationEntity);
        checkApplication(now, responseApplication, AppSettingsEnum.NO_SETTINGS);
    }
    
    @Test
    public void testConvertFromAppEntitySimpleApp() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationEntity applicationEntity;

        //init
        applicationEntity = new ApplicationEntity();
        applicationEntity.setCreatedAt(nowDate);
        applicationEntity.setDescription(APPLICATION_DESCRIPTION);
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION_GROUP)));
        applicationEntity.setId(APPLICATION_ID);
        applicationEntity.setName(APPLICATION_NAME);
        applicationEntity.setStatus(APPLICATION_STATUS);
        applicationEntity.setType(APPLICATION_TYPE);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION_USER_ID);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationEntity.setPrimaryOwner(primaryOwner);
        
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings simpleAppEntitySetings = new SimpleApplicationSettings();
        simpleAppEntitySetings.setClientId(APPLICATION_SIMPLE_CLIENT_ID);
        simpleAppEntitySetings.setType(APPLICATION_SIMPLE_TYPE);
        settings.setApp(simpleAppEntitySetings);
        applicationEntity.setSettings(settings);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationEntity);
        checkApplication(now, responseApplication, AppSettingsEnum.SIMPLE_SETTINGS);
    }

    @Test
    public void testConvertFromAppEntityOAuthClient() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationEntity applicationEntity;

        //init
        applicationEntity = new ApplicationEntity();
        applicationEntity.setCreatedAt(nowDate);
        applicationEntity.setDescription(APPLICATION_DESCRIPTION );
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION_GROUP)));
        applicationEntity.setId(APPLICATION_ID);
        applicationEntity.setName(APPLICATION_NAME);
        applicationEntity.setStatus(APPLICATION_STATUS);
        applicationEntity.setType(APPLICATION_TYPE);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION_USER_ID);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationEntity.setPrimaryOwner(primaryOwner);
        
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientEntitySettings = new OAuthClientSettings();
        oAuthClientEntitySettings.setApplicationType(APPLICATION_OAUTH_APPLICATION_TYPE);
        oAuthClientEntitySettings.setClientId(APPLICATION_OAUTH_CLIENT_ID);
        oAuthClientEntitySettings.setClientSecret(APPLICATION_OAUTH_CLIENT_SECRET);
        oAuthClientEntitySettings.setClientUri(APPLICATION_OAUTH_CLIENT_URI);
        oAuthClientEntitySettings.setGrantTypes(Arrays.asList(APPLICATION_OAUTH_GRANT_TYPE));
        oAuthClientEntitySettings.setLogoUri(APPLICATION_OAUTH_LOGO_URI);
        oAuthClientEntitySettings.setRedirectUris(Arrays.asList(APPLICATION_OAUTH_REDIRECT_URI));
        oAuthClientEntitySettings.setRenewClientSecretSupported(true);
        oAuthClientEntitySettings.setResponseTypes(Arrays.asList(APPLICATION_OAUTH_RESPONSE_TYPE));
        
        settings.setoAuthClient(oAuthClientEntitySettings);
        applicationEntity.setSettings(settings);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationEntity);
        checkApplication(now, responseApplication, AppSettingsEnum.OAUTH_SETTINGS);
    }
    
    private void checkApplication(Instant now, Application responseApplication, AppSettingsEnum appSettingsType) {
        assertNotNull(responseApplication);
        
        assertNull(responseApplication.getLinks());
        
        assertEquals(APPLICATION_TYPE, responseApplication.getApplicationType());
        assertEquals(now.toEpochMilli(), responseApplication.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(APPLICATION_DESCRIPTION, responseApplication.getDescription());
        assertEquals(APPLICATION_ID, responseApplication.getId());
        assertEquals(APPLICATION_NAME, responseApplication.getName());
        assertEquals(now.toEpochMilli(), responseApplication.getUpdatedAt().toInstant().toEpochMilli());
        
        List<String> groups = responseApplication.getGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(APPLICATION_GROUP, groups.get(0));
        
        User owner = responseApplication.getOwner();
        assertNotNull(owner);
        assertEquals(APPLICATION_USER_ID, owner.getId());
        
        io.gravitee.rest.api.portal.rest.model.ApplicationSettings applicationSettings = responseApplication.getSettings();
        if(AppSettingsEnum.NO_SETTINGS == appSettingsType) {
            assertNull(applicationSettings);
        } else {
            io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings sas = applicationSettings.getApp();
            io.gravitee.rest.api.portal.rest.model.OAuthClientSettings oacs = applicationSettings.getOauth();
            
            if(AppSettingsEnum.OAUTH_SETTINGS == appSettingsType) {
                    assertNull(sas);
                    assertNotNull(oacs);
                    assertEquals(APPLICATION_OAUTH_APPLICATION_TYPE, oacs.getApplicationType());
                    assertEquals(APPLICATION_OAUTH_CLIENT_ID, oacs.getClientId());
                    assertEquals(APPLICATION_OAUTH_CLIENT_SECRET, oacs.getClientSecret());
                    assertEquals(APPLICATION_OAUTH_CLIENT_URI, oacs.getClientUri());
                    assertEquals(APPLICATION_OAUTH_LOGO_URI, oacs.getLogoUri());
                    assertEquals(Boolean.TRUE, oacs.getRenewClientSecretSupported());
                    
                    final List<String> grantTypes = oacs.getGrantTypes();
                    assertNotNull(grantTypes);
                    assertFalse(grantTypes.isEmpty());
                    assertEquals(APPLICATION_OAUTH_GRANT_TYPE, grantTypes.get(0));
                    
                    final List<String> redirectUris = oacs.getRedirectUris();
                    assertNotNull(redirectUris);
                    assertFalse(redirectUris.isEmpty());
                    assertEquals(APPLICATION_OAUTH_REDIRECT_URI, redirectUris.get(0));
                    
                    final List<String> responseTypes = oacs.getResponseTypes();
                    assertNotNull(responseTypes);
                    assertFalse(responseTypes.isEmpty());
                    assertEquals(APPLICATION_OAUTH_RESPONSE_TYPE, responseTypes.get(0));
            } else if(AppSettingsEnum.SIMPLE_SETTINGS == appSettingsType) {
                    assertNotNull(sas);
                    assertNull(oacs);
                    assertEquals(APPLICATION_SIMPLE_CLIENT_ID, sas.getClientId());
                    assertEquals(APPLICATION_SIMPLE_TYPE, sas.getType());
            }
        }
    }
}

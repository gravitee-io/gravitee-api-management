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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationListItemSettings;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationLinks;
import io.gravitee.rest.api.portal.rest.model.Person;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationMapperTest {

    private static final String APPLICATION = "my-application";

        
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
        applicationListItem.setDescription(APPLICATION);
        applicationListItem.setGroups(new HashSet<String>(Arrays.asList(APPLICATION)));
        applicationListItem.setId(APPLICATION);
        applicationListItem.setName(APPLICATION);
        applicationListItem.setStatus(APPLICATION);
        applicationListItem.setType(APPLICATION);
        applicationListItem.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationListItem.setPrimaryOwner(primaryOwner);
        
        ApplicationListItemSettings settings = new ApplicationListItemSettings();
        settings.setClientId(APPLICATION);
        settings.setType(APPLICATION);
        applicationListItem.setSettings(settings);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationListItem);
        checkApplication(now, responseApplication, AppSettingsEnum.SIMPLE_SETTINGS);
    }
    
    @Test
    public void testConvertFromAppListItemWithoutSettings() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationListItem applicationListItem;

        //init
        applicationListItem = new ApplicationListItem();
        applicationListItem.setCreatedAt(nowDate);
        applicationListItem.setDescription(APPLICATION);
        applicationListItem.setGroups(new HashSet<String>(Arrays.asList(APPLICATION)));
        applicationListItem.setId(APPLICATION);
        applicationListItem.setName(APPLICATION);
        applicationListItem.setStatus(APPLICATION);
        applicationListItem.setType(APPLICATION);
        applicationListItem.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationListItem.setPrimaryOwner(primaryOwner);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationListItem);
        checkApplication(now, responseApplication, AppSettingsEnum.NO_SETTINGS);
    }
    
    @Test
    public void testApplicationLinks() {
        String basePath = "/"+APPLICATION;
        
        ApplicationLinks links = applicationMapper.computeApplicationLinks(basePath);
        
        assertNotNull(links);
        
        assertEquals(basePath, links.getSelf());
        assertEquals(basePath+"/analytics", links.getAnalytics());
        assertEquals(basePath+"/logs", links.getLogs());
        assertEquals(basePath+"/members", links.getMembers());
        assertEquals(basePath+"/metrics", links.getMetrics());
        assertEquals(basePath+"/notifications", links.getNotifications());
        assertEquals(basePath+"/picture", links.getPicture());
        assertEquals(basePath+"/plans", links.getPlans());
    }
    
    @Test
    public void testConvertFromAppEntityNoSettings() {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        ApplicationEntity applicationEntity;

        //init
        applicationEntity = new ApplicationEntity();
        applicationEntity.setCreatedAt(nowDate);
        applicationEntity.setDescription(APPLICATION);
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION)));
        applicationEntity.setId(APPLICATION);
        applicationEntity.setName(APPLICATION);
        applicationEntity.setStatus(APPLICATION);
        applicationEntity.setType(APPLICATION);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION);
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
        applicationEntity.setDescription(APPLICATION);
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION)));
        applicationEntity.setId(APPLICATION);
        applicationEntity.setName(APPLICATION);
        applicationEntity.setStatus(APPLICATION);
        applicationEntity.setType(APPLICATION);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationEntity.setPrimaryOwner(primaryOwner);
        
        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings simpleAppEntitySetings = new SimpleApplicationSettings();
        simpleAppEntitySetings.setClientId(APPLICATION);
        simpleAppEntitySetings.setType(APPLICATION);
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
        applicationEntity.setDescription(APPLICATION);
        applicationEntity.setGroups(new HashSet<String>(Arrays.asList(APPLICATION)));
        applicationEntity.setId(APPLICATION);
        applicationEntity.setName(APPLICATION);
        applicationEntity.setStatus(APPLICATION);
        applicationEntity.setType(APPLICATION);
        applicationEntity.setUpdatedAt(nowDate);
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(APPLICATION);
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        applicationEntity.setPrimaryOwner(primaryOwner);
        
        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings oAuthClientEntitySettings = new OAuthClientSettings();
        oAuthClientEntitySettings.setApplicationType(APPLICATION);
        oAuthClientEntitySettings.setClientId(APPLICATION);
        oAuthClientEntitySettings.setClientSecret(APPLICATION);
        oAuthClientEntitySettings.setClientUri(APPLICATION);
        oAuthClientEntitySettings.setGrantTypes(Arrays.asList(APPLICATION));
        oAuthClientEntitySettings.setLogoUri(APPLICATION);
        oAuthClientEntitySettings.setRedirectUris(Arrays.asList(APPLICATION));
        oAuthClientEntitySettings.setRenewClientSecretSupported(true);
        oAuthClientEntitySettings.setResponseTypes(Arrays.asList(APPLICATION));
        
        settings.setoAuthClient(oAuthClientEntitySettings);
        applicationEntity.setSettings(settings);
        
        //Test
        Application responseApplication = applicationMapper.convert(applicationEntity);
        checkApplication(now, responseApplication, AppSettingsEnum.OAUTH_SETTINGS);
    }
    
    private void checkApplication(Instant now, Application responseApplication, AppSettingsEnum appSettingsType) {
        assertNotNull(responseApplication);
        
        assertNull(responseApplication.getAnalytics());
        assertNull(responseApplication.getLogs());
        assertNull(responseApplication.getMembers());
        assertNull(responseApplication.getMetrics());
        assertNull(responseApplication.getNotifications());
        assertNull(responseApplication.getPlans());
        
        assertNull(responseApplication.getLinks());
        
        assertEquals(APPLICATION, responseApplication.getApplicationType());
        assertEquals(now.toEpochMilli(), responseApplication.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(APPLICATION, responseApplication.getDescription());
        assertEquals(APPLICATION, responseApplication.getId());
        assertEquals(APPLICATION, responseApplication.getName());
        assertEquals(APPLICATION, responseApplication.getStatus());
        assertEquals(now.toEpochMilli(), responseApplication.getUpdatedAt().toInstant().toEpochMilli());
        
        List<String> groups = responseApplication.getGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals(APPLICATION, groups.get(0));
        
        Person owner = responseApplication.getOwner();
        assertNotNull(owner);
        assertEquals(APPLICATION, owner.getId());
        
        io.gravitee.rest.api.portal.rest.model.ApplicationSettings applicationSettings = responseApplication.getSettings();
        if(AppSettingsEnum.NO_SETTINGS == appSettingsType) {
            assertNull(applicationSettings);
        } else {
            io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings sas = applicationSettings.getApp();
            io.gravitee.rest.api.portal.rest.model.OAuthClientSettings oacs = applicationSettings.getOauth();
            
            if(AppSettingsEnum.OAUTH_SETTINGS == appSettingsType) {
                    assertNull(sas);
                    assertNotNull(oacs);
                    assertEquals(APPLICATION, oacs.getApplicationType());
                    assertEquals(APPLICATION, oacs.getClientId());
                    assertEquals(APPLICATION, oacs.getClientSecret());
                    assertEquals(APPLICATION, oacs.getClientUri());
                    assertEquals(APPLICATION, oacs.getLogoUri());
                    assertEquals(Boolean.TRUE, oacs.getRenewClientSecretSupported());
                    
                    final List<String> grantTypes = oacs.getGrantTypes();
                    assertNotNull(grantTypes);
                    assertFalse(grantTypes.isEmpty());
                    assertEquals(APPLICATION, grantTypes.get(0));
                    
                    final List<String> redirectUris = oacs.getRedirectUris();
                    assertNotNull(redirectUris);
                    assertFalse(redirectUris.isEmpty());
                    assertEquals(APPLICATION, redirectUris.get(0));
                    
                    final List<String> responseTypes = oacs.getResponseTypes();
                    assertNotNull(responseTypes);
                    assertFalse(responseTypes.isEmpty());
                    assertEquals(APPLICATION, responseTypes.get(0));
            } else if(AppSettingsEnum.SIMPLE_SETTINGS == appSettingsType) {
                    assertNotNull(sas);
                    assertNull(oacs);
                    assertEquals(APPLICATION, sas.getClientId());
                    assertEquals(APPLICATION, sas.getType());
            }
        }
    }
}

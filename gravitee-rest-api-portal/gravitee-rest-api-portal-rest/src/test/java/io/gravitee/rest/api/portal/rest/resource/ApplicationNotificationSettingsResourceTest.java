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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.GenericNotificationConfig;
import io.gravitee.rest.api.portal.rest.model.NotificationConfig;
import io.gravitee.rest.api.portal.rest.model.NotificationsResponse;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationConfig;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationNotificationSettingsResourceTest extends AbstractResourceTest {
    
    @Override
    protected String contextPath() {
        return "applications/";
    }
    
    private static final String APPLICATION = "my-application";
    private static final String NOTIFICATION = "my-notification";
    
    @Before
    public void init() {
        resetAllMocks();
    }
    
    @Test
    public void shouldGetOnlyPortalNotifications() {
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.CREATE, RolePermissionAction.UPDATE, RolePermissionAction.DELETE);
        doReturn(new PortalNotificationConfigEntity()).when(portalNotificationConfigService).findById(USER_NAME, NotificationReferenceType.APPLICATION, APPLICATION);
        
        final Response response = target(APPLICATION).path("notifications").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(genericNotificationConfigService, Mockito.never()) .findByReference(NotificationReferenceType.APPLICATION, APPLICATION);
        Mockito.verify(portalNotificationConfigService).findById(USER_NAME, NotificationReferenceType.APPLICATION, APPLICATION);
        Mockito.verify(notificationConfigMapper, Mockito.never()).convert(any(GenericNotificationConfigEntity.class));
        Mockito.verify(notificationConfigMapper).convert(any(PortalNotificationConfigEntity.class));
        
        NotificationsResponse notificationsResponse = response.readEntity(NotificationsResponse.class);
        assertNotNull(notificationsResponse);
        List<NotificationConfig> notifs = notificationsResponse.getData();
        assertNotNull(notifs);
        assertEquals(1, notifs.size());
    }
    
    @Test
    public void shouldGetAllNotifications() {
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.CREATE, RolePermissionAction.UPDATE, RolePermissionAction.DELETE);
        doReturn(new PortalNotificationConfigEntity()).when(portalNotificationConfigService).findById(USER_NAME, NotificationReferenceType.APPLICATION, APPLICATION);
        doReturn(Arrays.asList(new GenericNotificationConfigEntity())).when(genericNotificationConfigService).findByReference(NotificationReferenceType.APPLICATION, APPLICATION);
        
        final Response response = target(APPLICATION).path("notifications").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(genericNotificationConfigService).findByReference(NotificationReferenceType.APPLICATION, APPLICATION);
        Mockito.verify(portalNotificationConfigService).findById(USER_NAME, NotificationReferenceType.APPLICATION, APPLICATION);
        Mockito.verify(notificationConfigMapper).convert(any(GenericNotificationConfigEntity.class));
        Mockito.verify(notificationConfigMapper).convert(any(PortalNotificationConfigEntity.class));
        
        NotificationsResponse notificationsResponse = response.readEntity(NotificationsResponse.class);
        assertNotNull(notificationsResponse);
        List<NotificationConfig> notifs = notificationsResponse.getData();
        assertNotNull(notifs);
        assertEquals(2, notifs.size());
    }
    
    @Test
    public void shouldCreateGenericNotification() {
        GenericNotificationConfig genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setName(NOTIFICATION);
        genericConfigInput.setConfigType("GENERIC");
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setHooks(Arrays.asList(NOTIFICATION));
        genericConfigInput.setId(NOTIFICATION);
        genericConfigInput.setNotifier(NOTIFICATION);
        genericConfigInput.setConfig(NOTIFICATION);
        genericConfigInput.setUseSystemProxy(Boolean.TRUE);
        
        GenericNotificationConfigEntity configEntity = new GenericNotificationConfigEntity();
        doReturn(configEntity).when(notificationConfigMapper).convert(genericConfigInput);
        
        GenericNotificationConfigEntity createdConfigEntity = new GenericNotificationConfigEntity();
        doReturn(createdConfigEntity).when(genericNotificationConfigService).create(configEntity);
        
        GenericNotificationConfig createdConfig = new GenericNotificationConfig();
        createdConfig.setConfigType("GENERIC");
        doReturn(createdConfig).when(notificationConfigMapper).convert(createdConfigEntity);
        
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.CREATE);
        
        final Response response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        
        Mockito.verify(notificationConfigMapper).convert(genericConfigInput);
        Mockito.verify(genericNotificationConfigService).create(configEntity);
        Mockito.verify(notificationConfigMapper).convert(createdConfigEntity);
        
        GenericNotificationConfig notifResponse = response.readEntity(GenericNotificationConfig.class);
        assertNotNull(notifResponse);
        assertEquals(createdConfig, notifResponse);
    }
    
    @Test
    public void shouldCreatePortalNotification() {
        GenericNotificationConfig genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setName(NOTIFICATION);
        genericConfigInput.setConfigType("PORTAL");
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setHooks(Arrays.asList(NOTIFICATION));
        
        PortalNotificationConfigEntity configEntity = new PortalNotificationConfigEntity();
        doReturn(configEntity).when(notificationConfigMapper).convertToPortalConfigEntity(genericConfigInput, USER_NAME);
        
        PortalNotificationConfigEntity createdConfigEntity = new PortalNotificationConfigEntity();
        doReturn(createdConfigEntity).when(portalNotificationConfigService).save(configEntity);
        
        PortalNotificationConfig createdConfig = new PortalNotificationConfig();
        createdConfig.setConfigType("GENERIC");
        doReturn(createdConfig).when(notificationConfigMapper).convert(createdConfigEntity);
        
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.READ);
        
        final Response response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        
        Mockito.verify(notificationConfigMapper).convertToPortalConfigEntity(genericConfigInput, USER_NAME);
        Mockito.verify(portalNotificationConfigService).save(configEntity);
        Mockito.verify(notificationConfigMapper).convert(createdConfigEntity);
        
        PortalNotificationConfig notifResponse = response.readEntity(PortalNotificationConfig.class);
        assertNotNull(notifResponse);
        assertEquals(createdConfig, notifResponse);
    }
    
    @Test
    public void shouldHaveForbiddenWhileCreatingNotification() {
        // !applicationId.equals(genericConfig.getReferenceId()
        GenericNotificationConfig genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId("FOO");
        genericConfigInput.setReferenceType("APPLICATION");
        Response response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !NotificationReferenceType.APPLICATION.name().equals(genericConfig.getReferenceType())
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("FOO");
        response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // GENERIC without permission
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("GENERIC");
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.CREATE);
        response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // PORTAL without permission
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("PORTAL");
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_NOTIFICATION, APPLICATION, RolePermissionAction.READ);
        response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // wrong configType
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("FOO");
        response = target(APPLICATION).path("notifications").request().post(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
    }
    
    @Test
    public void shouldUpdateGenericNotification() {
        GenericNotificationConfig genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setName(NOTIFICATION);
        genericConfigInput.setConfigType("GENERIC");
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setHooks(Arrays.asList(NOTIFICATION));
        genericConfigInput.setId(NOTIFICATION);
        genericConfigInput.setNotifier(NOTIFICATION);
        genericConfigInput.setConfig(NOTIFICATION);
        genericConfigInput.setUseSystemProxy(Boolean.TRUE);
        
        GenericNotificationConfigEntity configEntity = new GenericNotificationConfigEntity();
        doReturn(configEntity).when(notificationConfigMapper).convert(genericConfigInput);
        
        GenericNotificationConfigEntity updatedConfigEntity = new GenericNotificationConfigEntity();
        doReturn(updatedConfigEntity).when(genericNotificationConfigService).update(configEntity);
        
        GenericNotificationConfig updatedConfig = new GenericNotificationConfig();
        updatedConfig.setConfigType("GENERIC");
        doReturn(updatedConfig).when(notificationConfigMapper).convert(updatedConfigEntity);
        
        final Response response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().put(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(notificationConfigMapper).convert(genericConfigInput);
        Mockito.verify(genericNotificationConfigService).update(configEntity);
        Mockito.verify(notificationConfigMapper).convert(updatedConfigEntity);
        
        GenericNotificationConfig notifResponse = response.readEntity(GenericNotificationConfig.class);
        assertNotNull(notifResponse);
        assertEquals(updatedConfig, notifResponse);
    }
    
    @Test
    public void shouldHaveForbiddenWhileUpdatingGenericNotification() {
        // !applicationId.equals(genericConfig.getReferenceId()
        GenericNotificationConfig genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId("FOO");
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("GENERIC");
        genericConfigInput.setId(NOTIFICATION);

        Response response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().put(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !NotificationReferenceType.APPLICATION.name().equals(genericConfig.getReferenceType())
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("FOO");
        genericConfigInput.setConfigType("GENERIC");
        genericConfigInput.setId(NOTIFICATION);
        
        response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().put(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !NotificationConfigType.GENERIC.name().equals(genericConfig.getConfigType()
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("FOO");
        genericConfigInput.setId(NOTIFICATION);
        
        response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().put(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !notificationId.equals(genericConfig.getId())
        genericConfigInput = new GenericNotificationConfig();
        genericConfigInput.setReferenceId(APPLICATION);
        genericConfigInput.setReferenceType("APPLICATION");
        genericConfigInput.setConfigType("GENERIC");
        genericConfigInput.setId("FOO");
        response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().put(Entity.json(genericConfigInput));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
    }
    
    @Test
    public void shouldUpdatePortalNotification() {
        PortalNotificationConfig portalNotificationConfig = new PortalNotificationConfig();
        portalNotificationConfig.setName(NOTIFICATION);
        portalNotificationConfig.setConfigType("PORTAL");
        portalNotificationConfig.setReferenceType("APPLICATION");
        portalNotificationConfig.setReferenceId(APPLICATION);
        portalNotificationConfig.setHooks(Arrays.asList(NOTIFICATION));
        
        PortalNotificationConfigEntity configEntity = new PortalNotificationConfigEntity();
        doReturn(configEntity).when(notificationConfigMapper).convert(any(PortalNotificationConfig.class));
        
        PortalNotificationConfigEntity updatedConfigEntity = new PortalNotificationConfigEntity();
        doReturn(updatedConfigEntity).when(portalNotificationConfigService).save(configEntity);
        
        PortalNotificationConfig updatedConfig = new PortalNotificationConfig();
        updatedConfig.setConfigType("PORTAL");
        doReturn(updatedConfig).when(notificationConfigMapper).convert(updatedConfigEntity);
        
        final Response response = target(APPLICATION).path("notifications").request().put(Entity.json(portalNotificationConfig));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ArgumentCaptor<PortalNotificationConfig> portalNotifCapture = ArgumentCaptor.forClass(PortalNotificationConfig.class);
        Mockito.verify(notificationConfigMapper).convert(portalNotifCapture.capture());
        PortalNotificationConfig capturedInput = portalNotifCapture.getValue();
        assertEquals(NOTIFICATION, capturedInput.getName());
        assertEquals("PORTAL", capturedInput.getConfigType());
        assertEquals("APPLICATION", capturedInput.getReferenceType());
        assertEquals(APPLICATION, capturedInput.getReferenceId());
        assertEquals(Arrays.asList(NOTIFICATION), capturedInput.getHooks());
        assertEquals(USER_NAME, capturedInput.getUser());
        
        Mockito.verify(portalNotificationConfigService).save(configEntity);
        Mockito.verify(notificationConfigMapper).convert(updatedConfigEntity);
        
        PortalNotificationConfig notifResponse = response.readEntity(PortalNotificationConfig.class);
        assertNotNull(notifResponse);
        assertEquals(updatedConfig, notifResponse);
    }
    
    @Test
    public void shouldHaveForbiddenWhileUpdatingPortalNotification() {
        // !applicationId.equals(portalConfig.getReferenceId())
        PortalNotificationConfig portalNotificationConfig = new PortalNotificationConfig();
        portalNotificationConfig.setReferenceId("FOO");
        portalNotificationConfig.setReferenceType("APPLICATION");
        portalNotificationConfig.setConfigType("PORTAL");

        Response response = target(APPLICATION).path("notifications").request().put(Entity.json(portalNotificationConfig));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !NotificationReferenceType.APPLICATION.name().equals(portalConfig.getReferenceType())
        portalNotificationConfig = new PortalNotificationConfig();
        portalNotificationConfig.setReferenceId(APPLICATION);
        portalNotificationConfig.setReferenceType("FOO");
        portalNotificationConfig.setConfigType("PORTAL");
        
        response = target(APPLICATION).path("notifications").request().put(Entity.json(portalNotificationConfig));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
        // !NotificationConfigType.PORTAL.name().equals(portalConfig.getConfigType())
        portalNotificationConfig = new PortalNotificationConfig();
        portalNotificationConfig.setReferenceId(APPLICATION);
        portalNotificationConfig.setReferenceType("APPLICATION");
        portalNotificationConfig.setConfigType("FOO");
        
        response = target(APPLICATION).path("notifications").request().put(Entity.json(portalNotificationConfig));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        
    }
    
    @Test
    public void shouldDeleteNotifications() {
        
        final Response response = target(APPLICATION).path("notifications").path(NOTIFICATION).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        Mockito.verify(genericNotificationConfigService).delete(NOTIFICATION);
    }
}

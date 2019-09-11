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

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.portal.rest.model.GenericNotificationConfig;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationConfig;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationConfigMapperTest {

    private static final String PORTAL_NOTIF_HOOK = "my-portal-notif-hook";
    private static final String PORTAL_NOTIF_REF_ID = "my-portal-notif-ref-id";
    private static final String PORTAL_NOTIF_REF_TYPE = "my-portal-notif-ref-type";
    private static final String PORTAL_NOTIF_USER = "my-portal-notif-user";

    private static final String GENERIC_NOTIF_CONFIG = "my-generic-notif-config";
    private static final String GENERIC_NOTIF_HOOK = "my-generic-notif-hook";
    private static final String GENERIC_NOTIF_ID = "my-generic-notif-id";
    private static final String GENERIC_NOTIF_NAME = "my-generic-notif-name";
    private static final String GENERIC_NOTIF_NOTIFIER = "my-generic-notif-notifier";
    private static final String GENERIC_NOTIF_REF_ID = "my-generic-notif-ref-id";
    private static final String GENERIC_NOTIF_REF_TYPE = "my-generic-notif-ref-type";

    private NotificationConfigMapper notificationConfigMapper = new NotificationConfigMapper();
    
    private GenericNotificationConfigEntity genericConfigEntity;
    private PortalNotificationConfigEntity portalConfigEntity;

    @Before
    public void init() {
        genericConfigEntity = new GenericNotificationConfigEntity();
        genericConfigEntity.setConfig(GENERIC_NOTIF_CONFIG);
        genericConfigEntity.setConfigType(NotificationConfigType.GENERIC);
        genericConfigEntity.setHooks(Arrays.asList(GENERIC_NOTIF_HOOK));
        genericConfigEntity.setId(GENERIC_NOTIF_ID);
        genericConfigEntity.setName(GENERIC_NOTIF_NAME);
        genericConfigEntity.setNotifier(GENERIC_NOTIF_NOTIFIER);
        genericConfigEntity.setReferenceId(GENERIC_NOTIF_REF_ID);
        genericConfigEntity.setReferenceType(GENERIC_NOTIF_REF_TYPE);
        genericConfigEntity.setUseSystemProxy(false);
        
        portalConfigEntity = new PortalNotificationConfigEntity();
        portalConfigEntity.setConfigType(NotificationConfigType.PORTAL);
        portalConfigEntity.setHooks(Arrays.asList(PORTAL_NOTIF_HOOK));
        portalConfigEntity.setReferenceId(PORTAL_NOTIF_REF_ID);
        portalConfigEntity.setReferenceType(PORTAL_NOTIF_REF_TYPE);
        portalConfigEntity.setUser(PORTAL_NOTIF_USER);
    }

    @Test
    public void testConvertGenericToGeneric() {
        GenericNotificationConfig genericConfig = notificationConfigMapper.convert(genericConfigEntity);
        assertEquals(GENERIC_NOTIF_CONFIG, genericConfig.getConfig());
        assertEquals("GENERIC", genericConfig.getConfigType());
        List<String> hooks = genericConfig.getHooks();
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertEquals(GENERIC_NOTIF_HOOK, hooks.get(0));
        assertEquals(GENERIC_NOTIF_ID, genericConfig.getId());
        assertEquals(GENERIC_NOTIF_NAME, genericConfig.getName());
        assertEquals(GENERIC_NOTIF_NOTIFIER, genericConfig.getNotifier());
        assertEquals(GENERIC_NOTIF_REF_ID, genericConfig.getReferenceId());
        assertEquals(GENERIC_NOTIF_REF_TYPE, genericConfig.getReferenceType());
        assertFalse(genericConfig.getUseSystemProxy());
        
        genericConfigEntity = notificationConfigMapper.convert(genericConfig);
        assertEquals(GENERIC_NOTIF_CONFIG, genericConfigEntity.getConfig());
        assertEquals(NotificationConfigType.GENERIC, genericConfigEntity.getConfigType());
        hooks = genericConfigEntity.getHooks();
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertEquals(GENERIC_NOTIF_HOOK, hooks.get(0));
        assertEquals(GENERIC_NOTIF_ID, genericConfigEntity.getId());
        assertEquals(GENERIC_NOTIF_NAME, genericConfigEntity.getName());
        assertEquals(GENERIC_NOTIF_NOTIFIER, genericConfigEntity.getNotifier());
        assertEquals(GENERIC_NOTIF_REF_ID, genericConfigEntity.getReferenceId());
        assertEquals(GENERIC_NOTIF_REF_TYPE, genericConfigEntity.getReferenceType());
        assertFalse(genericConfigEntity.isUseSystemProxy());
    }
    
    @Test
    public void testConvertPortalToPortal() {
        PortalNotificationConfig portalConfig = notificationConfigMapper.convert(portalConfigEntity);
        assertEquals(PORTAL_NOTIF_USER, portalConfig.getUser());
        assertEquals("PORTAL", portalConfig.getConfigType());
        List<String> hooks = portalConfig.getHooks();
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertEquals(PORTAL_NOTIF_HOOK, hooks.get(0));
        assertEquals("Portal Notification", portalConfig.getName());
        assertEquals(PORTAL_NOTIF_REF_ID, portalConfig.getReferenceId());
        assertEquals(PORTAL_NOTIF_REF_TYPE, portalConfig.getReferenceType());
        
        portalConfigEntity = notificationConfigMapper.convert(portalConfig);
        assertEquals(PORTAL_NOTIF_USER, portalConfigEntity.getUser());
        assertEquals(NotificationConfigType.PORTAL, portalConfigEntity.getConfigType());
        hooks = portalConfigEntity.getHooks();
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertEquals(PORTAL_NOTIF_HOOK, hooks.get(0));
        assertEquals(PORTAL_NOTIF_REF_ID, portalConfigEntity.getReferenceId());
        assertEquals(PORTAL_NOTIF_REF_TYPE, portalConfigEntity.getReferenceType());
    }
    
    @Test
    public void testConvertGenericToPortalEntity() {
        GenericNotificationConfig genericConfig = new GenericNotificationConfig();
        genericConfig.setConfig(GENERIC_NOTIF_CONFIG);
        genericConfig.setConfigType("GENERIC");
        genericConfig.setHooks(Arrays.asList(GENERIC_NOTIF_HOOK));
        genericConfig.setId(GENERIC_NOTIF_ID);
        genericConfig.setName(GENERIC_NOTIF_NAME);
        genericConfig.setNotifier(GENERIC_NOTIF_NOTIFIER);
        genericConfig.setReferenceId(GENERIC_NOTIF_REF_ID);
        genericConfig.setReferenceType(GENERIC_NOTIF_REF_TYPE);
        genericConfig.setUseSystemProxy(false);
        
        PortalNotificationConfigEntity portalNotificationConfigEntity = notificationConfigMapper.convertToPortalConfigEntity(genericConfig, PORTAL_NOTIF_USER);
        assertEquals(PORTAL_NOTIF_USER, portalNotificationConfigEntity.getUser());
        //APIPortal: should it be PORTAL ?
        assertEquals(NotificationConfigType.GENERIC, portalNotificationConfigEntity.getConfigType());
        List<String> hooks = portalNotificationConfigEntity.getHooks();
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertEquals(GENERIC_NOTIF_HOOK, hooks.get(0));
        assertEquals(GENERIC_NOTIF_REF_ID, portalNotificationConfigEntity.getReferenceId());
        assertEquals(GENERIC_NOTIF_REF_TYPE, portalNotificationConfigEntity.getReferenceType());
    }
}

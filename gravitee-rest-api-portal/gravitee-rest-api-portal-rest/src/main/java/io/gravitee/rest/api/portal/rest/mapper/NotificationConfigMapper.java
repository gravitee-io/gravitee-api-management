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

import org.springframework.stereotype.Component;

import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.portal.rest.model.GenericNotificationConfig;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationConfig;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */

@Component
public class NotificationConfigMapper {
    public PortalNotificationConfig convert(PortalNotificationConfigEntity notificationConfigEntity) {
        PortalNotificationConfig notificationConfig = new PortalNotificationConfig();
        notificationConfig.setName("Portal Notification");
        notificationConfig.setConfigType(notificationConfigEntity.getConfigType().name());
        notificationConfig.setReferenceId(notificationConfigEntity.getReferenceId());
        notificationConfig.setReferenceType(notificationConfigEntity.getReferenceType());
        notificationConfig.setHooks(notificationConfigEntity.getHooks());
        notificationConfig.setUser(notificationConfigEntity.getUser());
        return notificationConfig;
    }

    public GenericNotificationConfig convert(GenericNotificationConfigEntity notificationConfigEntity) {
        GenericNotificationConfig notificationConfig = new GenericNotificationConfig();
        notificationConfig.setName(notificationConfigEntity.getName());
        notificationConfig.setConfigType(notificationConfigEntity.getConfigType().name());
        notificationConfig.setReferenceId(notificationConfigEntity.getReferenceId());
        notificationConfig.setReferenceType(notificationConfigEntity.getReferenceType());
        notificationConfig.setHooks(notificationConfigEntity.getHooks());
        notificationConfig.setId(notificationConfigEntity.getId());
        notificationConfig.setConfig(notificationConfigEntity.getConfig());
        notificationConfig.setNotifier(notificationConfigEntity.getNotifier());
        notificationConfig.setUseSystemProxy(notificationConfigEntity.isUseSystemProxy());
        return notificationConfig;
    }
    
    public PortalNotificationConfigEntity convert(PortalNotificationConfig notificationConfig) {
        PortalNotificationConfigEntity notificationConfigEntity = new PortalNotificationConfigEntity();
        notificationConfigEntity.setConfigType(NotificationConfigType.valueOf(notificationConfig.getConfigType()));
        notificationConfigEntity.setReferenceId(notificationConfig.getReferenceId());
        notificationConfigEntity.setReferenceType(notificationConfig.getReferenceType());
        notificationConfigEntity.setHooks(notificationConfig.getHooks());
        notificationConfigEntity.setUser(notificationConfig.getUser());
        return notificationConfigEntity;
    }
    
    public PortalNotificationConfigEntity convertToPortalConfigEntity(GenericNotificationConfig notificationConfig, String user) {
        PortalNotificationConfigEntity notificationConfigEntity = new PortalNotificationConfigEntity();
        notificationConfigEntity.setConfigType(NotificationConfigType.valueOf(notificationConfig.getConfigType()));
        notificationConfigEntity.setReferenceId(notificationConfig.getReferenceId());
        notificationConfigEntity.setReferenceType(notificationConfig.getReferenceType());
        notificationConfigEntity.setHooks(notificationConfig.getHooks());
        notificationConfigEntity.setUser(user);
        return notificationConfigEntity;
    }

    public GenericNotificationConfigEntity convert(GenericNotificationConfig notificationConfig) {
        GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
        notificationConfigEntity.setName(notificationConfig.getName());
        notificationConfigEntity.setConfigType(NotificationConfigType.valueOf(notificationConfig.getConfigType()));
        notificationConfigEntity.setReferenceId(notificationConfig.getReferenceId());
        notificationConfigEntity.setReferenceType(notificationConfig.getReferenceType());
        notificationConfigEntity.setHooks(notificationConfig.getHooks());
        notificationConfigEntity.setId(notificationConfig.getId());
        notificationConfigEntity.setConfig(notificationConfig.getConfig());
        notificationConfigEntity.setNotifier(notificationConfig.getNotifier());
        notificationConfigEntity.setUseSystemProxy(notificationConfig.getUseSystemProxy());
        return notificationConfigEntity;
    }

}

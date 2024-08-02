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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PortalNotificationConfigServiceImpl extends AbstractService implements PortalNotificationConfigService {

    private final Logger LOGGER = LoggerFactory.getLogger(PortalNotificationConfigServiceImpl.class);

    @Lazy
    @Autowired
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Override
    public PortalNotificationConfigEntity save(PortalNotificationConfigEntity notificationEntity) {
        try {
            if (notificationEntity.getHooks() == null || notificationEntity.getHooks().isEmpty()) {
                portalNotificationConfigRepository.delete(convert(notificationEntity));
                return getDefaultEmpty(
                    notificationEntity.getUser(),
                    NotificationReferenceType.valueOf(notificationEntity.getReferenceType()),
                    notificationEntity.getReferenceId()
                );
            } else {
                Optional<PortalNotificationConfig> optionalConfig = portalNotificationConfigRepository.findById(
                    notificationEntity.getUser(),
                    NotificationReferenceType.valueOf(notificationEntity.getReferenceType()),
                    notificationEntity.getReferenceId()
                );
                PortalNotificationConfig notificationConfig = convert(notificationEntity);

                if (optionalConfig.isPresent()) {
                    notificationConfig.setCreatedAt(optionalConfig.get().getCreatedAt());
                    notificationConfig.setUpdatedAt(new Date());
                    return convert(portalNotificationConfigRepository.update(notificationConfig));
                } else {
                    notificationConfig.setCreatedAt(new Date());
                    notificationConfig.setUpdatedAt(notificationConfig.getCreatedAt());
                    return convert(portalNotificationConfigRepository.create(notificationConfig));
                }
            }
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to save the notification settings {}", notificationEntity, te);
            throw new TechnicalManagementException(
                "An error occurs while trying to save the notification settings " + notificationEntity,
                te
            );
        }
    }

    @Override
    public PortalNotificationConfigEntity findById(String user, NotificationReferenceType referenceType, String referenceId) {
        try {
            Optional<PortalNotificationConfig> optionalConfig = portalNotificationConfigRepository.findById(
                user,
                referenceType,
                referenceId
            );
            if (optionalConfig.isPresent()) {
                return convert(optionalConfig.get());
            }
            return getDefaultEmpty(user, referenceType, referenceId);
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to get the notification settings {}/{}/{}", user, referenceType, referenceId, te);
            throw new TechnicalManagementException(
                "An error occurs while trying to get the notification settings " + user + "/" + referenceType + "/" + referenceId,
                te
            );
        }
    }

    @Override
    public void deleteByUser(String user) {
        try {
            portalNotificationConfigRepository.deleteByUser(user);
        } catch (TechnicalException te) {
            LOGGER.error("An error occurs while trying to delete notification settings for user {}", user, te);
            throw new TechnicalManagementException("An error occurs while trying to delete notification settings for user " + user, te);
        }
    }

    @Override
    public void deleteReference(NotificationReferenceType referenceType, String referenceId) {
        try {
            portalNotificationConfigRepository.deleteByReferenceIdAndReferenceType(referenceId, referenceType);
        } catch (TechnicalException te) {
            LOGGER.error(
                "An error occurs while trying to delete notification settings for reference {} / {}",
                referenceType,
                referenceId,
                te
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to delete notification settings for reference " + referenceType + " / " + referenceId,
                te
            );
        }
    }

    private PortalNotificationConfigEntity getDefaultEmpty(String user, NotificationReferenceType referenceType, String referenceId) {
        PortalNotificationConfigEntity portalNotificationConfigEntity = new PortalNotificationConfigEntity();
        portalNotificationConfigEntity.setConfigType(NotificationConfigType.PORTAL);
        portalNotificationConfigEntity.setReferenceType(referenceType.name());
        portalNotificationConfigEntity.setReferenceId(referenceId);
        portalNotificationConfigEntity.setUser(user);
        portalNotificationConfigEntity.setHooks(Collections.emptyList());
        return portalNotificationConfigEntity;
    }

    private PortalNotificationConfig convert(PortalNotificationConfigEntity entity) {
        PortalNotificationConfig portalNotificationConfig = new PortalNotificationConfig();
        portalNotificationConfig.setReferenceType(NotificationReferenceType.valueOf(entity.getReferenceType()));
        portalNotificationConfig.setReferenceId(entity.getReferenceId());
        portalNotificationConfig.setUser(entity.getUser());
        portalNotificationConfig.setHooks(entity.getHooks());
        return portalNotificationConfig;
    }

    private PortalNotificationConfigEntity convert(PortalNotificationConfig portalNotificationConfig) {
        PortalNotificationConfigEntity entity = new PortalNotificationConfigEntity();
        entity.setConfigType(NotificationConfigType.PORTAL);
        entity.setReferenceType(portalNotificationConfig.getReferenceType().name());
        entity.setReferenceId(portalNotificationConfig.getReferenceId());
        entity.setUser(portalNotificationConfig.getUser());
        entity.setHooks(portalNotificationConfig.getHooks());
        return entity;
    }
}

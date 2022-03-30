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
package io.gravitee.rest.api.service.impl;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.rest.api.model.notification.NewPortalNotificationEntity;
import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PortalNotificationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PortalNotificationServiceImpl extends AbstractService implements PortalNotificationService {

    private final Logger LOGGER = LoggerFactory.getLogger(PortalNotificationServiceImpl.class);

    @Autowired
    private PortalNotificationRepository portalNotificationRepository;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Override
    public List<PortalNotificationEntity> findByUser(String user) {
        try {
            return portalNotificationRepository.findByUser(user).stream().map(this::convert).collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find notifications by user {}", user, ex);
            throw new TechnicalManagementException("An error occurs while trying to find notifications by username " + user, ex);
        }
    }

    @Override
    public PortalNotificationEntity findById(String notificationId) {
        try {
            Optional<PortalNotification> portalNotification = portalNotificationRepository.findById(notificationId);
            if (portalNotification.isPresent()) {
                return this.convert(portalNotification.get());
            }
            throw new PortalNotificationNotFoundException(notificationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find notification with id {}", notificationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find notification with id " + notificationId, ex);
        }
    }

    @Override
    public void create(ExecutionContext executionContext, Hook hook, List<String> users, Object params) {
        try {
            final String title = notificationTemplateService.resolveTemplateWithParam(
                executionContext.getOrganizationId(),
                hook.getTemplate() + ".PORTAL.TITLE",
                params
            );
            final String content = notificationTemplateService.resolveTemplateWithParam(
                executionContext.getOrganizationId(),
                hook.getTemplate() + ".PORTAL",
                params
            );

            List<NewPortalNotificationEntity> notifications = new ArrayList<>(users.size());
            users.forEach(
                user -> {
                    NewPortalNotificationEntity notification = new NewPortalNotificationEntity();
                    notification.setUser(user);
                    notification.setTitle(title);
                    notification.setMessage(content);
                    notifications.add(notification);
                }
            );

            create(notifications);
        } catch (final Exception ex) {
            LOGGER.error("Error while sending notification", ex);
            throw new TechnicalManagementException("Error while sending notification", ex);
        }
    }

    @Override
    public void deleteAll(String user) {
        try {
            portalNotificationRepository.deleteAll(user);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete all notifications for user  {}", user, ex);
            throw new TechnicalManagementException("An error occurs while trying delete all notifications for user " + user, ex);
        }
    }

    @Override
    public void delete(String notificationId) {
        try {
            portalNotificationRepository.delete(notificationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete {}", notificationId, ex);
            throw new TechnicalManagementException("An error occurs while trying delete " + notificationId, ex);
        }
    }

    private void create(List<NewPortalNotificationEntity> notificationEntities) {
        final Date now = new Date();
        List<PortalNotification> notifications = notificationEntities.stream().map(this::convert).collect(Collectors.toList());
        notifications.forEach(
            n -> {
                n.setId(UuidString.generateRandom());
                n.setCreatedAt(now);
            }
        );
        try {
            portalNotificationRepository.create(notifications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {}", notifications, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + notifications, ex);
        }
    }

    private PortalNotification convert(NewPortalNotificationEntity entity) {
        PortalNotification notification = new PortalNotification();
        notification.setTitle(entity.getTitle());
        notification.setMessage(entity.getMessage());
        notification.setUser(entity.getUser());
        return notification;
    }

    private PortalNotificationEntity convert(PortalNotification notification) {
        PortalNotificationEntity entity = new PortalNotificationEntity();
        entity.setId(notification.getId());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setCreatedAt(notification.getCreatedAt());
        return entity;
    }
}

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
package io.gravitee.apim.infra.crud_service.portal_notification;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_notification.crud_service.PortalNotificationCrudService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import java.util.Date;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalNotificationCrudServiceImpl implements PortalNotificationCrudService {

    private final PortalNotificationRepository portalNotificationRepository;

    public PortalNotificationCrudServiceImpl(@Lazy PortalNotificationRepository portalNotificationRepository) {
        this.portalNotificationRepository = portalNotificationRepository;
    }

    @Override
    public boolean markAsRead(String user, String environmentId, String id, Date readAt) {
        try {
            return portalNotificationRepository.markAsRead(user, environmentId, id, readAt);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while marking notification " + id + " as read", e);
        }
    }

    @Override
    public void markAllAsRead(String user, String environmentId, Date readAt) {
        try {
            portalNotificationRepository.markAllAsRead(user, environmentId, readAt);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while marking the notifications of user " + user + " as read", e);
        }
    }

    @Override
    public boolean delete(String user, String environmentId, String id) {
        try {
            return portalNotificationRepository.delete(user, environmentId, id);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while deleting notification " + id, e);
        }
    }

    @Override
    public void deleteAll(String user, String environmentId) {
        try {
            portalNotificationRepository.deleteAll(user, environmentId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while deleting the notifications of user " + user, e);
        }
    }
}

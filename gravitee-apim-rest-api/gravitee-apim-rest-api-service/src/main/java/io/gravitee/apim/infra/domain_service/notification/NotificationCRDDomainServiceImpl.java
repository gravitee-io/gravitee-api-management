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
package io.gravitee.apim.infra.domain_service.notification;

import io.gravitee.apim.core.api.domain_service.NotificationCRDDomainService;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Service
public class NotificationCRDDomainServiceImpl implements NotificationCRDDomainService {

    private final PortalNotificationConfigService portalNotificationConfigService;

    @Override
    public void syncApiPortalNotifications(String apiId, String user, PortalNotificationConfigEntity notificationConfig) {
        syncPortalNotifications(apiId, NotificationReferenceType.API, user, notificationConfig);
    }

    public void syncPortalNotifications(
        String referenceId,
        NotificationReferenceType referenceType,
        String user,
        PortalNotificationConfigEntity notificationConfig
    ) {
        Objects.requireNonNull(referenceId);
        Objects.requireNonNull(user);

        if (notificationConfig != null) {
            // set GKO user as it is the primary user
            notificationConfig.setUser(user);
            notificationConfig.setReferenceId(referenceId);
            notificationConfig.setReferenceType(referenceType.name());
            portalNotificationConfigService.save(notificationConfig);
            return;
        }

        // no notification
        PortalNotificationConfigEntity existing = portalNotificationConfigService.findById(user, referenceType, referenceId);

        // there was a notification, need to delete the old one
        if (!existing.isDefaultEmpty()) {
            // this will discard all notifications for that API
            portalNotificationConfigService.save(PortalNotificationConfigEntity.newDefaultEmpty(user, referenceType.name(), referenceId));
        }
    }
}

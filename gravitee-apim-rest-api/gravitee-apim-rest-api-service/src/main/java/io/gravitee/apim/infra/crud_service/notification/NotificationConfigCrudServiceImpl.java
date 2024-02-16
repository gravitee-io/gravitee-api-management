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
package io.gravitee.apim.infra.crud_service.notification;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.notification.crud_service.NotificationConfigCrudService;
import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.apim.infra.adapter.NotificationConfigAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class NotificationConfigCrudServiceImpl implements NotificationConfigCrudService {

    private final GenericNotificationConfigRepository notificationConfigRepository;

    public NotificationConfigCrudServiceImpl(@Lazy GenericNotificationConfigRepository notificationConfigRepository) {
        this.notificationConfigRepository = notificationConfigRepository;
    }

    @Override
    public NotificationConfig create(NotificationConfig config) {
        try {
            var result = notificationConfigRepository.create(NotificationConfigAdapter.INSTANCE.toRepository(config));
            return NotificationConfigAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurs while trying to create the %s notification config of %s",
                    config.getReferenceType(),
                    config.getReferenceId()
                ),
                e
            );
        }
    }
}

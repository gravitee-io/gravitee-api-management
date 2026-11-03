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
package io.gravitee.apim.core.portal_notification.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_notification.crud_service.PortalNotificationCrudService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PortalNotificationNotFoundException;
import java.util.Date;

@UseCase
public class MarkUserPortalNotificationsAsReadUseCase {

    private final PortalNotificationCrudService portalNotificationCrudService;

    public MarkUserPortalNotificationsAsReadUseCase(PortalNotificationCrudService portalNotificationCrudService) {
        this.portalNotificationCrudService = portalNotificationCrudService;
    }

    public void execute(Input input) {
        var environmentId = input.executionContext().getEnvironmentId();
        var now = Date.from(TimeProvider.instantNow());
        if (input.notificationId() == null) {
            portalNotificationCrudService.markAllAsRead(input.user(), environmentId, now);
        } else if (!portalNotificationCrudService.markAsRead(input.user(), environmentId, input.notificationId(), now)) {
            throw new PortalNotificationNotFoundException(input.notificationId());
        }
    }

    /**
     * Without {@code notificationId}, every unread notification of the user in that environment is marked as read.
     */
    public record Input(ExecutionContext executionContext, String user, String notificationId) {}
}

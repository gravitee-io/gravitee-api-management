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
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PortalNotificationNotFoundException;

@UseCase
public class DeleteUserPortalNotificationsUseCase {

    private final PortalNotificationCrudService portalNotificationCrudService;

    public DeleteUserPortalNotificationsUseCase(PortalNotificationCrudService portalNotificationCrudService) {
        this.portalNotificationCrudService = portalNotificationCrudService;
    }

    public void execute(Input input) {
        var environmentId = input.executionContext().getEnvironmentId();
        if (input.notificationId() == null) {
            portalNotificationCrudService.deleteAll(input.user(), environmentId);
        } else if (!portalNotificationCrudService.delete(input.user(), environmentId, input.notificationId())) {
            throw new PortalNotificationNotFoundException(input.notificationId());
        }
    }

    /**
     * Without {@code notificationId}, every notification of the user in that environment is deleted.
     */
    public record Input(ExecutionContext executionContext, String user, String notificationId) {}
}

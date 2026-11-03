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
import io.gravitee.apim.core.portal_notification.domain_service.PortalNotificationNamesDomainService;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationQuery;
import io.gravitee.apim.core.portal_notification.query_service.PortalNotificationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Objects;

@UseCase
public class SearchUserPortalNotificationsUseCase {

    private final PortalNotificationQueryService portalNotificationQueryService;
    private final PortalNotificationNamesDomainService portalNotificationNames;

    public SearchUserPortalNotificationsUseCase(
        PortalNotificationQueryService portalNotificationQueryService,
        PortalNotificationNamesDomainService portalNotificationNames
    ) {
        this.portalNotificationQueryService = portalNotificationQueryService;
        this.portalNotificationNames = portalNotificationNames;
    }

    public Output execute(Input input) {
        var environmentId = input.executionContext().getEnvironmentId();
        var notifications = portalNotificationQueryService.search(
            PortalNotificationQuery.builder()
                .user(input.user())
                .environmentId(environmentId)
                .read(input.read())
                .apiId(input.apiId())
                .applicationId(input.applicationId())
                .build(),
            input.pageable()
        );

        var content = notifications.getContent();
        return new Output(
            notifications,
            portalNotificationNames.apiNames(
                content.stream().map(PortalNotification::getApiId).filter(Objects::nonNull).toList(),
                environmentId
            ),
            portalNotificationNames.applicationNames(
                content.stream().map(PortalNotification::getApplicationId).filter(Objects::nonNull).toList(),
                environmentId
            )
        );
    }

    public record Input(
        ExecutionContext executionContext,
        String user,
        Boolean read,
        String apiId,
        String applicationId,
        Pageable pageable
    ) {}

    /**
     * {@code apiNames} and {@code applicationNames} have no entry for an API or an application deleted since.
     */
    public record Output(Page<PortalNotification> notifications, Map<String, String> apiNames, Map<String, String> applicationNames) {}
}

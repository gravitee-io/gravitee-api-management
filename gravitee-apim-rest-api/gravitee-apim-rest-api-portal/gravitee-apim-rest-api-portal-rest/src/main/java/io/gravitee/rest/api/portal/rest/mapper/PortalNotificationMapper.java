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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.portal_notification.use_case.GetUserPortalNotificationFiltersUseCase;
import io.gravitee.apim.core.portal_notification.use_case.SearchUserPortalNotificationsUseCase;
import io.gravitee.rest.api.portal.rest.model.PortalNotification;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationFilters;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationReference;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PortalNotificationMapper {

    public List<PortalNotification> convert(SearchUserPortalNotificationsUseCase.Output output) {
        return output
            .notifications()
            .getContent()
            .stream()
            .map(notification ->
                new PortalNotification()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .createdAt(notification.getCreatedAt().toInstant().atOffset(ZoneOffset.UTC))
                    .read(notification.isRead())
                    .hook(notification.getHook())
                    .api(reference(notification.getApiId(), output.apiNames()))
                    .application(reference(notification.getApplicationId(), output.applicationNames()))
            )
            .toList();
    }

    public PortalNotificationFilters convert(GetUserPortalNotificationFiltersUseCase.Output output) {
        return new PortalNotificationFilters()
            .apis(
                output
                    .apis()
                    .stream()
                    .map(api -> new PortalNotificationReference().id(api.id()).name(api.name()))
                    .toList()
            )
            .applications(
                output
                    .applications()
                    .stream()
                    .map(application -> new PortalNotificationReference().id(application.id()).name(application.name()))
                    .toList()
            );
    }

    private static PortalNotificationReference reference(String id, Map<String, String> names) {
        return id == null ? null : new PortalNotificationReference().id(id).name(names.get(id));
    }
}

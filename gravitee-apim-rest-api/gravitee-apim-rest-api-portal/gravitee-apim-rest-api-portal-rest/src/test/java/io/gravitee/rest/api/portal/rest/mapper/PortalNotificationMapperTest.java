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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_notification.model.NotificationReference;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.use_case.GetUserPortalNotificationFiltersUseCase;
import io.gravitee.apim.core.portal_notification.use_case.SearchUserPortalNotificationsUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.portal.rest.model.PortalNotificationReference;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortalNotificationMapperTest {

    private final PortalNotificationMapper mapper = new PortalNotificationMapper();

    @Test
    void should_convert_notifications_with_read_state_and_named_references() {
        var createdAt = Instant.parse("2026-10-30T10:00:00Z");
        var notification = PortalNotification.builder()
            .id("n-1")
            .title("title")
            .message("message")
            .createdAt(Date.from(createdAt))
            .readAt(Date.from(createdAt))
            .hook("APIKEY_REVOKED")
            .apiId("api-1")
            .applicationId("deleted-app")
            .build();

        var result = mapper.convert(
            new SearchUserPortalNotificationsUseCase.Output(
                new Page<>(List.of(notification), 1, 1, 1),
                Map.of("api-1", "Payments"),
                Map.of()
            )
        );

        var converted = result.getFirst();
        assertThat(converted.getId()).isEqualTo("n-1");
        assertThat(converted.getTitle()).isEqualTo("title");
        assertThat(converted.getMessage()).isEqualTo("message");
        assertThat(converted.getCreatedAt()).isEqualTo(createdAt.atOffset(ZoneOffset.UTC));
        assertThat(converted.getRead()).isTrue();
        assertThat(converted.getHook()).isEqualTo("APIKEY_REVOKED");
        assertThat(converted.getApi()).isEqualTo(new PortalNotificationReference().id("api-1").name("Payments"));
        assertThat(converted.getApplication()).isEqualTo(new PortalNotificationReference().id("deleted-app").name(null));
    }

    @Test
    void should_convert_legacy_notification_without_references() {
        var notification = PortalNotification.builder().id("legacy").createdAt(new Date()).build();

        var converted = mapper
            .convert(new SearchUserPortalNotificationsUseCase.Output(new Page<>(List.of(notification), 1, 1, 1), Map.of(), Map.of()))
            .getFirst();

        assertThat(converted.getRead()).isFalse();
        assertThat(converted.getHook()).isNull();
        assertThat(converted.getApi()).isNull();
        assertThat(converted.getApplication()).isNull();
    }

    @Test
    void should_convert_filters() {
        var result = mapper.convert(
            new GetUserPortalNotificationFiltersUseCase.Output(
                List.of(new NotificationReference("api-1", "Payments")),
                List.of(new NotificationReference("app-1", null))
            )
        );

        assertThat(result.getApis()).containsExactly(new PortalNotificationReference().id("api-1").name("Payments"));
        assertThat(result.getApplications()).containsExactly(new PortalNotificationReference().id("app-1").name(null));
    }
}

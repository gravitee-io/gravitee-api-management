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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.PortalNotificationServicesInMemory;
import io.gravitee.apim.core.portal_notification.domain_service.PortalNotificationNamesDomainService;
import io.gravitee.apim.core.portal_notification.model.NotificationReference;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.PortalNotificationNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserPortalNotificationsUseCasesTest {

    private static final String USER = "user-id";
    private static final ExecutionContext CONTEXT = new ExecutionContext("org-id", "env-1");
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    private final PortalNotificationServicesInMemory portalNotifications = new PortalNotificationServicesInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id("api-1").name("Payments").environmentId("env-1").build()));
        applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id("app-1").name("Shop").environmentId("env-1").build()));
        portalNotifications.initWith(
            List.of(
                notification("n-1", 5).apiId("api-1").applicationId("app-1").hook("SUBSCRIPTION_ACCEPTED").build(),
                notification("n-2", 4).apiId("deleted-api").applicationId("deleted-app").readAt(new Date(1000)).build(),
                notification("n-3", 3).build(),
                notification("other-env", 2).environmentId("env-2").apiId("api-other").build(),
                notification("other-user", 1).user("someone-else").apiId("api-1").build(),
                notification("legacy", 0).environmentId(null).build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Nested
    class Search {

        private final SearchUserPortalNotificationsUseCase cut = new SearchUserPortalNotificationsUseCase(
            portalNotifications,
            new PortalNotificationNamesDomainService(apiCrudService, applicationCrudService)
        );

        @Test
        void should_return_page_of_notifications_of_user_in_environment_with_names() {
            var output = cut.execute(
                new SearchUserPortalNotificationsUseCase.Input(CONTEXT, USER, null, null, null, new PageableImpl(1, 10))
            );

            assertThat(output.notifications().getContent())
                .extracting(PortalNotification::getId)
                .containsExactly("n-1", "n-2", "n-3", "legacy");
            assertThat(output.notifications().getTotalElements()).isEqualTo(4);
            assertThat(output.apiNames()).containsExactlyEntriesOf(Map.of("api-1", "Payments"));
            assertThat(output.applicationNames()).containsExactlyEntriesOf(Map.of("app-1", "Shop"));
        }

        @Test
        void should_pass_filters_and_paging() {
            var output = cut.execute(
                new SearchUserPortalNotificationsUseCase.Input(CONTEXT, USER, false, null, null, new PageableImpl(2, 1))
            );

            assertThat(output.notifications().getContent()).extracting(PortalNotification::getId).containsExactly("n-3");
            assertThat(output.notifications().getTotalElements()).isEqualTo(3);
            assertThat(output.notifications().getPageNumber()).isEqualTo(2);
        }

        @Test
        void should_filter_by_api_and_application() {
            var output = cut.execute(
                new SearchUserPortalNotificationsUseCase.Input(CONTEXT, USER, null, "api-1", "app-1", new PageableImpl(1, 10))
            );

            assertThat(output.notifications().getContent()).extracting(PortalNotification::getId).containsExactly("n-1");
        }
    }

    @Nested
    class Filters {

        private final GetUserPortalNotificationFiltersUseCase cut = new GetUserPortalNotificationFiltersUseCase(
            portalNotifications,
            new PortalNotificationNamesDomainService(apiCrudService, applicationCrudService)
        );

        @Test
        void should_return_apis_and_applications_of_scope_named_and_deleted_last() {
            var output = cut.execute(new GetUserPortalNotificationFiltersUseCase.Input(CONTEXT, USER));

            assertThat(output.apis()).containsExactly(
                new NotificationReference("api-1", "Payments"),
                new NotificationReference("deleted-api", null)
            );
            assertThat(output.applications()).containsExactly(
                new NotificationReference("app-1", "Shop"),
                new NotificationReference("deleted-app", null)
            );
        }
    }

    @Nested
    class MarkAsRead {

        private final MarkUserPortalNotificationsAsReadUseCase cut = new MarkUserPortalNotificationsAsReadUseCase(portalNotifications);

        @Test
        void should_mark_one_notification_as_read_now() {
            cut.execute(new MarkUserPortalNotificationsAsReadUseCase.Input(CONTEXT, USER, "n-1"));

            assertThat(find("n-1").getReadAt()).isEqualTo(Date.from(NOW));
        }

        @Test
        void should_throw_not_found_for_notification_of_another_environment() {
            var throwable = catchThrowable(() ->
                cut.execute(new MarkUserPortalNotificationsAsReadUseCase.Input(CONTEXT, USER, "other-env"))
            );

            assertThat(throwable).isInstanceOf(PortalNotificationNotFoundException.class);
            assertThat(find("other-env").getReadAt()).isNull();
        }

        @Test
        void should_throw_not_found_for_notification_of_another_user() {
            var throwable = catchThrowable(() ->
                cut.execute(new MarkUserPortalNotificationsAsReadUseCase.Input(CONTEXT, USER, "other-user"))
            );

            assertThat(throwable).isInstanceOf(PortalNotificationNotFoundException.class);
            assertThat(find("other-user").getReadAt()).isNull();
        }

        @Test
        void should_mark_all_notifications_of_scope_as_read() {
            cut.execute(new MarkUserPortalNotificationsAsReadUseCase.Input(CONTEXT, USER, null));

            assertThat(List.of(find("n-1"), find("n-3"), find("legacy"))).allSatisfy(n ->
                assertThat(n.getReadAt()).isEqualTo(Date.from(NOW))
            );
            assertThat(find("n-2").getReadAt()).isEqualTo(new Date(1000));
            assertThat(find("other-env").getReadAt()).isNull();
            assertThat(find("other-user").getReadAt()).isNull();
        }
    }

    @Nested
    class Delete {

        private final DeleteUserPortalNotificationsUseCase cut = new DeleteUserPortalNotificationsUseCase(portalNotifications);

        @Test
        void should_delete_one_notification() {
            cut.execute(new DeleteUserPortalNotificationsUseCase.Input(CONTEXT, USER, "n-1"));

            assertThat(portalNotifications.storage()).extracting(PortalNotification::getId).doesNotContain("n-1");
        }

        @Test
        void should_throw_not_found_for_notification_of_another_environment() {
            var throwable = catchThrowable(() -> cut.execute(new DeleteUserPortalNotificationsUseCase.Input(CONTEXT, USER, "other-env")));

            assertThat(throwable).isInstanceOf(PortalNotificationNotFoundException.class);
            assertThat(portalNotifications.storage()).extracting(PortalNotification::getId).contains("other-env");
        }

        @Test
        void should_delete_all_notifications_of_scope_only() {
            cut.execute(new DeleteUserPortalNotificationsUseCase.Input(CONTEXT, USER, null));

            assertThat(portalNotifications.storage())
                .extracting(PortalNotification::getId)
                .containsExactlyInAnyOrder("other-env", "other-user");
        }
    }

    private PortalNotification find(String id) {
        return portalNotifications
            .storage()
            .stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }

    private static PortalNotification.PortalNotificationBuilder notification(String id, int minutesAfterEpoch) {
        return PortalNotification.builder()
            .id(id)
            .title("title-" + id)
            .message("message-" + id)
            .user(USER)
            .environmentId("env-1")
            .createdAt(new Date(minutesAfterEpoch * 60_000L));
    }
}

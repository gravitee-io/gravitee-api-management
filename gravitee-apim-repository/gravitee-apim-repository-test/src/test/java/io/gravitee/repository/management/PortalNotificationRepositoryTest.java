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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.PortalNotificationSearchCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.management.model.PortalNotificationReferences;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PortalNotificationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalnotification-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final PortalNotification notification = new PortalNotification();
        notification.setId("notif-create");
        notification.setTitle("notif-title");
        notification.setMessage("notif-message");
        notification.setUser("notif-userId");
        notification.setCreatedAt(new Date(1439022010883L));

        PortalNotification notificationCreated = portalNotificationRepository.create(notification);

        assertEquals(notification.getId(), notificationCreated.getId());
        assertEquals(notification.getTitle(), notificationCreated.getTitle());
        assertEquals(notification.getMessage(), notificationCreated.getMessage());
        assertEquals(notification.getUser(), notificationCreated.getUser());
        assertTrue(compareDate(notification.getCreatedAt(), notificationCreated.getCreatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        assertEquals(1, portalNotificationRepository.findByUser("notif-userId-toDelete").size());
        portalNotificationRepository.delete("notif-toDelete");
        assertTrue(portalNotificationRepository.findByUser("notif-userId-toDelete").isEmpty());
    }

    @Test
    public void shouldDeleteAllUser() throws Exception {
        assertEquals(1, portalNotificationRepository.findByUser("notif-userId-toDelete").size());
        portalNotificationRepository.deleteAll("notif-userId-toDelete");
        assertTrue(portalNotificationRepository.findByUser("notif-userId-toDelete").isEmpty());
    }

    @Test
    public void shouldFindByUserId() throws Exception {
        List<PortalNotification> notifications = portalNotificationRepository.findByUser("notif-userId-findByUserId");

        assertEquals(1, notifications.size());
        PortalNotification notification = notifications.get(0);
        assertEquals("notif-findByUserId", notification.getId());
        assertEquals("notif-title-findByUserId", notification.getTitle());
        assertEquals("notif-message-findByUserId", notification.getMessage());
        assertEquals("notif-userId-findByUserId", notification.getUser());
        assertTrue(compareDate(new Date(1439022010883L), notification.getCreatedAt()));
    }

    @Test
    public void shouldNotFindByUsername() throws Exception {
        List<PortalNotification> notifications = portalNotificationRepository.findByUser("unknown");

        assertTrue(notifications.isEmpty());
    }

    @Test
    public void should_create_with_read_state_and_references() throws Exception {
        final PortalNotification notification = new PortalNotification();
        notification.setId("notif-create-full");
        notification.setTitle("notif-title");
        notification.setMessage("notif-message");
        notification.setUser("notif-userId");
        notification.setCreatedAt(new Date(1439022010883L));
        notification.setReadAt(new Date(1439022020883L));
        notification.setHook("SUBSCRIPTION_ACCEPTED");
        notification.setApiId("api-id");
        notification.setApplicationId("app-id");
        notification.setEnvironmentId("env-id");

        portalNotificationRepository.create(notification);

        PortalNotification found = portalNotificationRepository.findById("notif-create-full").orElseThrow();
        assertThat(found.getHook()).isEqualTo("SUBSCRIPTION_ACCEPTED");
        assertThat(found.getApiId()).isEqualTo("api-id");
        assertThat(found.getApplicationId()).isEqualTo("app-id");
        assertThat(found.getEnvironmentId()).isEqualTo("env-id");
        assertTrue(compareDate(new Date(1439022020883L), found.getReadAt()));
    }

    @Nested
    class Search {

        @Test
        void should_return_user_notifications_of_environment_and_without_environment_newest_first() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().build(), firstPage(10));

            assertThat(page.getContent())
                .extracting(PortalNotification::getId)
                .containsExactly("inbox-1", "inbox-2", "inbox-3", "inbox-legacy");
            assertThat(page.getTotalElements()).isEqualTo(4);
        }

        @Test
        void should_scope_to_another_environment() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().environmentId("env-2").build(), firstPage(10));

            assertThat(page.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-other-env", "inbox-legacy");
        }

        @Test
        void should_filter_unread() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().read(false).build(), firstPage(10));

            assertThat(page.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-1", "inbox-3", "inbox-legacy");
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        void should_filter_read() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().read(true).build(), firstPage(10));

            assertThat(page.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-2");
        }

        @Test
        void should_filter_by_api() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().apiId("api-1").build(), firstPage(10));

            assertThat(page.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-1", "inbox-3");
        }

        @Test
        void should_filter_by_application() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().applicationId("app-1").build(), firstPage(10));

            assertThat(page.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-1", "inbox-2");
        }

        @Test
        void should_combine_filters() throws Exception {
            assertThat(
                portalNotificationRepository.search(criteria().apiId("api-1").applicationId("app-1").build(), firstPage(10)).getContent()
            )
                .extracting(PortalNotification::getId)
                .containsExactly("inbox-1");
            assertThat(
                portalNotificationRepository.search(criteria().apiId("api-2").read(false).build(), firstPage(10)).getContent()
            ).isEmpty();
        }

        @Test
        void should_page_with_total_from_database() throws Exception {
            Page<PortalNotification> first = portalNotificationRepository.search(
                criteria().build(),
                new PageableBuilder().pageNumber(0).pageSize(2).build()
            );
            Page<PortalNotification> second = portalNotificationRepository.search(
                criteria().build(),
                new PageableBuilder().pageNumber(1).pageSize(2).build()
            );

            assertThat(first.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-1", "inbox-2");
            assertThat(first.getTotalElements()).isEqualTo(4);
            assertThat(second.getContent()).extracting(PortalNotification::getId).containsExactly("inbox-3", "inbox-legacy");
            assertThat(second.getTotalElements()).isEqualTo(4);
        }

        @Test
        void should_return_legacy_notification_without_new_fields() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().build(), firstPage(10));

            PortalNotification legacy = page.getContent().get(3);
            assertThat(legacy.getId()).isEqualTo("inbox-legacy");
            assertThat(legacy.getReadAt()).isNull();
            assertThat(legacy.getHook()).isNull();
            assertThat(legacy.getApiId()).isNull();
            assertThat(legacy.getApplicationId()).isNull();
            assertThat(legacy.getEnvironmentId()).isNull();
        }

        @Test
        void should_return_nothing_for_unknown_user() throws Exception {
            Page<PortalNotification> page = portalNotificationRepository.search(criteria().user("unknown").build(), firstPage(10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    class MarkAsRead {

        @Test
        void should_mark_own_notification_as_read() throws Exception {
            boolean updated = portalNotificationRepository.markAsRead("inbox-user", "env-1", "inbox-1", new Date(1700000009000L));

            assertThat(updated).isTrue();
            assertTrue(compareDate(new Date(1700000009000L), portalNotificationRepository.findById("inbox-1").orElseThrow().getReadAt()));
        }

        @Test
        void should_keep_first_read_date_of_notification_already_read() throws Exception {
            boolean updated = portalNotificationRepository.markAsRead("inbox-user", "env-1", "inbox-2", new Date(1700000009000L));

            assertThat(updated).isTrue();
            assertTrue(compareDate(new Date(1700000004500L), portalNotificationRepository.findById("inbox-2").orElseThrow().getReadAt()));
        }

        @Test
        void should_mark_notification_without_environment_as_read() throws Exception {
            assertThat(portalNotificationRepository.markAsRead("inbox-user", "env-1", "inbox-legacy", new Date())).isTrue();
        }

        @Test
        void should_not_mark_notification_of_another_environment() throws Exception {
            boolean updated = portalNotificationRepository.markAsRead("inbox-user", "env-1", "inbox-other-env", new Date());

            assertThat(updated).isFalse();
            assertThat(portalNotificationRepository.findById("inbox-other-env").orElseThrow().getReadAt()).isNull();
        }

        @Test
        void should_not_mark_notification_of_another_user() throws Exception {
            boolean updated = portalNotificationRepository.markAsRead("inbox-user", "env-1", "inbox-other-user", new Date());

            assertThat(updated).isFalse();
            assertThat(portalNotificationRepository.findById("inbox-other-user").orElseThrow().getReadAt()).isNull();
        }

        @Test
        void should_mark_all_unread_notifications_of_scope_as_read() throws Exception {
            portalNotificationRepository.markAllAsRead("inbox-user", "env-1", new Date(1700000009000L));

            assertThat(portalNotificationRepository.search(criteria().read(false).build(), firstPage(10)).getContent()).isEmpty();
            assertTrue(compareDate(new Date(1700000004500L), portalNotificationRepository.findById("inbox-2").orElseThrow().getReadAt()));
            assertThat(portalNotificationRepository.findById("inbox-other-env").orElseThrow().getReadAt()).isNull();
            assertThat(portalNotificationRepository.findById("inbox-other-user").orElseThrow().getReadAt()).isNull();
        }
    }

    @Nested
    class ScopedDelete {

        @Test
        void should_delete_own_notification() throws Exception {
            assertThat(portalNotificationRepository.delete("inbox-user", "env-1", "inbox-3")).isTrue();
            assertThat(portalNotificationRepository.findById("inbox-3")).isEmpty();
        }

        @Test
        void should_not_delete_notification_of_another_environment() throws Exception {
            assertThat(portalNotificationRepository.delete("inbox-user", "env-1", "inbox-other-env")).isFalse();
            assertThat(portalNotificationRepository.findById("inbox-other-env")).isPresent();
        }

        @Test
        void should_not_delete_notification_of_another_user() throws Exception {
            assertThat(portalNotificationRepository.delete("inbox-user", "env-1", "inbox-other-user")).isFalse();
            assertThat(portalNotificationRepository.findById("inbox-other-user")).isPresent();
        }

        @Test
        void should_delete_all_notifications_of_scope_only() throws Exception {
            portalNotificationRepository.deleteAll("inbox-user", "env-1");

            assertThat(portalNotificationRepository.findByUser("inbox-user"))
                .extracting(PortalNotification::getId)
                .containsExactly("inbox-other-env");
            assertThat(portalNotificationRepository.findById("inbox-other-user")).isPresent();
        }
    }

    @Test
    public void should_find_distinct_references_of_scope() throws Exception {
        PortalNotificationReferences references = portalNotificationRepository.findDistinctReferences("inbox-user", "env-1");

        assertThat(references.apiIds()).containsExactlyInAnyOrder("api-1", "api-2");
        assertThat(references.applicationIds()).containsExactlyInAnyOrder("app-1", "app-2");
    }

    private static PortalNotificationSearchCriteria.PortalNotificationSearchCriteriaBuilder criteria() {
        return PortalNotificationSearchCriteria.builder().user("inbox-user").environmentId("env-1");
    }

    private static Pageable firstPage(int size) {
        return new PageableBuilder().pageNumber(0).pageSize(size).build();
    }

    @Test
    public void shouldFindById() throws Exception {
        Optional<PortalNotification> notification = portalNotificationRepository.findById("notif-findById");

        assertTrue(notification.isPresent());
        PortalNotification portalNotification = notification.get();
        assertEquals("notif-findById", portalNotification.getId());
        assertEquals("notif-title-findById", portalNotification.getTitle());
        assertEquals("notif-message-findById", portalNotification.getMessage());
        assertEquals("notif-userId-findById", portalNotification.getUser());
        assertTrue(compareDate(new Date(1439022010883L), portalNotification.getCreatedAt()));
    }
}

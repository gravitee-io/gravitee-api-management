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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.gravitee.definition.model.Origin;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class PortalNotificationConfigRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalnotificationConfig-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final PortalNotificationConfig expected = PortalNotificationConfig
            .builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("config-created")
            .user("userid")
            .hooks(Arrays.asList("A", "B", "C"))
            .groups(Set.of("1", "2", "3"))
            .organizationId("org1")
            .updatedAt(new Date(1439022010883L))
            .createdAt(new Date(1439022010883L))
            .origin(Origin.MANAGEMENT)
            .build();

        PortalNotificationConfig notificationCreated = portalNotificationConfigRepository.create(expected);

        assertNotification(notificationCreated, expected);
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(portalNotificationConfigRepository.findById("userD", NotificationReferenceType.API, "config-to-delete").isPresent());

        final PortalNotificationConfig cfg = PortalNotificationConfig
            .builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("config-to-delete")
            .user("userid")
            .build();

        portalNotificationConfigRepository.delete(cfg);

        assertThat(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-delete")).isNotPresent();
    }

    @Test
    public void shouldUpdate() throws Exception {
        final PortalNotificationConfig expected = PortalNotificationConfig
            .builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("config-to-update")
            .user("userE")
            .hooks(Arrays.asList("D", "B", "C"))
            .groups(Set.of("7", "8", "9"))
            .organizationId("org1")
            .updatedAt(new Date(1479022010883L))
            .createdAt(new Date(1469022010883L))
            .origin(Origin.MANAGEMENT)
            .build();

        PortalNotificationConfig notificationUpdated = portalNotificationConfigRepository.update(expected);

        assertNotification(notificationUpdated, expected);
    }

    @Test
    public void shouldFindById() throws Exception {
        final PortalNotificationConfig expected = PortalNotificationConfig
            .builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("config-to-find")
            .user("userF")
            .hooks(Arrays.asList("A", "B"))
            .groups(Set.of("1", "2"))
            .updatedAt(new Date(1439022010883L))
            .createdAt(new Date(1439022010883L))
            .origin(Origin.MANAGEMENT)
            .organizationId("org1")
            .build();

        Optional<PortalNotificationConfig> optNotificationFound = portalNotificationConfigRepository.findById(
            "userF",
            NotificationReferenceType.API,
            "config-to-find"
        );

        assertThat(optNotificationFound).isPresent();
        assertNotification(optNotificationFound.get(), expected);
    }

    @Test
    public void shouldKubernetesOriginById() throws Exception {
        final PortalNotificationConfig cfg = PortalNotificationConfig
            .builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("kubernetes-config-to-find")
            .user("userid")
            .hooks(Arrays.asList("A", "B"))
            .groups(Set.of("1", "2"))
            .updatedAt(new Date(1439022010883L))
            .createdAt(new Date(1439022010883L))
            .origin(Origin.KUBERNETES)
            .build();

        Optional<PortalNotificationConfig> optNotificationFound = portalNotificationConfigRepository.findById(
            "userid",
            NotificationReferenceType.API,
            "kubernetes-config-to-find"
        );

        assertThat(optNotificationFound).isPresent();
        assertNotification(optNotificationFound.get(), cfg);
    }

    @Test
    public void shouldNotFoundById() throws Exception {
        assertThat(portalNotificationConfigRepository.findById("userid-unknown", NotificationReferenceType.API, "config-to-find"))
            .isNotPresent();
        assertThat(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.APPLICATION, "config-to-find"))
            .isNotPresent();
        assertThat(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-not-find"))
            .isNotPresent();
    }

    @Test
    public void shouldFindByHookAndReference() throws Exception {
        List<PortalNotificationConfig> configs = portalNotificationConfigRepository.findByReferenceAndHook(
            "B",
            NotificationReferenceType.APPLICATION,
            "search"
        );

        assertThat(configs).as("size").hasSize(2);

        List<String> userIds = configs.stream().map(PortalNotificationConfig::getUser).toList();

        assertThat(userIds).containsExactlyInAnyOrder("userA", "userB");
    }

    @Test
    public void shouldNotFindByHookAndReference() throws Exception {
        List<PortalNotificationConfig> configs = portalNotificationConfigRepository.findByReferenceAndHook(
            "D",
            NotificationReferenceType.APPLICATION,
            "search"
        );

        assertThat(configs).as("size").isEmpty();
    }

    @Test
    public void shouldFindByHookAndOrganizationId() throws Exception {
        List<PortalNotificationConfig> configs = portalNotificationConfigRepository.findByHookAndOrganizationId("A", "org1");
        List<String> userIds = configs.stream().map(PortalNotificationConfig::getUser).toList();
        assertAll(() -> assertEquals("size", 2, configs.size()), () -> assertTrue(userIds.containsAll(List.of("userA", "userF"))));
    }

    @Test
    public void shouldNotFindByHookAndOrganizationId() throws Exception {
        List<PortalNotificationConfig> configs = portalNotificationConfigRepository.findByHookAndOrganizationId("A", "org4");
        assertTrue("size", configs.isEmpty());
    }

    @Test
    public void shouldDeleteByUser() throws Exception {
        portalNotificationConfigRepository.deleteByUser("useridToDelete");

        assertThat(portalNotificationConfigRepository.findById("useridToDelete", NotificationReferenceType.API, "config")).isNotPresent();
    }

    @Test
    public void shouldDeleteReference() throws Exception {
        assertThat(portalNotificationConfigRepository.findById("apiToDelete-1", NotificationReferenceType.API, "apiToDelete"))
            .as("should exist before delete {apiToDelete-1}")
            .isPresent();

        assertThat(portalNotificationConfigRepository.findById("apiToDelete-2", NotificationReferenceType.API, "apiToDelete"))
            .as("should exist before delete {apiToDelete-2}")
            .isPresent();

        portalNotificationConfigRepository.deleteByReferenceIdAndReferenceType("apiToDelete", NotificationReferenceType.API);

        assertThat(portalNotificationConfigRepository.findById("apiToDelete-1", NotificationReferenceType.API, "apiToDelete"))
            .as("should be deleted {apiToDelete-1}")
            .isNotPresent();

        assertThat(portalNotificationConfigRepository.findById("apiToDelete-2", NotificationReferenceType.API, "apiToDelete"))
            .as("should be deleted {apiToDelete-2}")
            .isNotPresent();
    }

    private static void assertNotification(PortalNotificationConfig actual, PortalNotificationConfig expected) {
        assertAll(
            () -> assertThat(actual.getReferenceType()).isEqualTo(expected.getReferenceType()),
            () -> assertThat(actual.getReferenceId()).isEqualTo(expected.getReferenceId()),
            () -> assertThat(actual.getUser()).isEqualTo(expected.getUser()),
            () -> assertThat(actual.getHooks()).containsExactlyInAnyOrderElementsOf(expected.getHooks()),
            () -> assertThat(actual.getGroups()).containsExactlyInAnyOrderElementsOf(expected.getGroups()),
            () -> assertThat(actual.getOrigin()).isEqualTo(expected.getOrigin()),
            () -> assertThat(actual.getOrganizationId()).isEqualTo(expected.getOrganizationId()),
            () -> assertThat(compareDate(actual.getCreatedAt(), expected.getCreatedAt())).isTrue(),
            () -> assertThat(compareDate(actual.getUpdatedAt(), expected.getUpdatedAt())).isTrue()
        );
    }
}

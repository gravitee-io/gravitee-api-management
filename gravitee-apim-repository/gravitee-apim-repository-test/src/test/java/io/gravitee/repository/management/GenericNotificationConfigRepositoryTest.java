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
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;

public class GenericNotificationConfigRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/genericnotificationConfig-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setId("new-id");
        cfg.setName("new config");
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-created");
        cfg.setNotifier("notifierId");
        cfg.setConfig("my new configuration");
        cfg.setUseSystemProxy(true);
        cfg.setHooks(Arrays.asList("A", "B", "C"));
        cfg.setUpdatedAt(new Date(1439022010883L));
        cfg.setCreatedAt(new Date(1439022010883L));

        GenericNotificationConfig notificationCreated = genericNotificationConfigRepository.create(cfg);

        assertEquals(cfg.getId(), notificationCreated.getId());
        assertEquals(cfg.getName(), notificationCreated.getName());
        assertEquals(cfg.getReferenceType(), notificationCreated.getReferenceType());
        assertEquals(cfg.getReferenceType(), notificationCreated.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationCreated.getReferenceId());
        assertEquals(cfg.getNotifier(), notificationCreated.getNotifier());
        assertEquals(cfg.getConfig(), notificationCreated.getConfig());
        assertEquals(cfg.getHooks(), notificationCreated.getHooks());
        assertTrue(compareDate(cfg.getCreatedAt(), notificationCreated.getCreatedAt()));
        assertTrue(compareDate(cfg.getUpdatedAt(), notificationCreated.getUpdatedAt()));
        assertTrue(cfg.isUseSystemProxy());
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(genericNotificationConfigRepository.findById("notif-to-delete").isPresent());
        genericNotificationConfigRepository.delete("notif-to-delete");
        assertFalse(genericNotificationConfigRepository.findById("notif-to-delete").isPresent());
    }

    @Test
    public void shouldUpdate() throws Exception {
        final GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setId("notif-to-update");
        cfg.setName("notif-updated");
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-to-update");
        cfg.setNotifier("notifierId");
        cfg.setConfig("updated configuration");
        cfg.setUseSystemProxy(true);
        cfg.setHooks(Arrays.asList("D", "B", "C"));
        cfg.setUpdatedAt(new Date(1479022010883L));
        cfg.setCreatedAt(new Date(1469022010883L));

        GenericNotificationConfig notificationUpdated = genericNotificationConfigRepository.update(cfg);

        assertEquals(cfg.getReferenceType(), notificationUpdated.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationUpdated.getReferenceId());
        assertEquals(cfg.getNotifier(), notificationUpdated.getNotifier());
        assertEquals(cfg.getConfig(), notificationUpdated.getConfig());
        assertTrue(cfg.getHooks().containsAll(notificationUpdated.getHooks()));
        assertTrue(compareDate(cfg.getCreatedAt(), notificationUpdated.getCreatedAt()));
        assertTrue(compareDate(cfg.getUpdatedAt(), notificationUpdated.getUpdatedAt()));
        assertTrue(cfg.isUseSystemProxy());
    }

    @Test
    public void shouldFindById() throws Exception {
        final GenericNotificationConfig cfg = new GenericNotificationConfig();
        cfg.setId("notif-to-find");
        cfg.setName("notif-to-find");
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-to-find");
        cfg.setNotifier("notifierId");
        cfg.setConfig("my config");
        cfg.setUseSystemProxy(true);
        cfg.setHooks(Arrays.asList("A", "B"));
        cfg.setUpdatedAt(new Date(1439022010883L));
        cfg.setCreatedAt(new Date(1439022010883L));

        Optional<GenericNotificationConfig> optNotificationFound = genericNotificationConfigRepository.findById("notif-to-find");

        assertTrue(optNotificationFound.isPresent());
        GenericNotificationConfig notificationFound = optNotificationFound.get();
        assertEquals(cfg.getReferenceType(), notificationFound.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationFound.getReferenceId());
        assertEquals(cfg.getNotifier(), notificationFound.getNotifier());
        assertEquals(cfg.getConfig(), notificationFound.getConfig());
        assertEquals(cfg.isUseSystemProxy(), notificationFound.isUseSystemProxy());
        assertEquals(cfg.getHooks(), notificationFound.getHooks());
        assertTrue(compareDate(cfg.getCreatedAt(), notificationFound.getCreatedAt()));
        assertTrue(compareDate(cfg.getUpdatedAt(), notificationFound.getUpdatedAt()));
    }

    @Test
    public void shouldNotFoundById() throws Exception {
        Optional<GenericNotificationConfig> optNotificationFound = genericNotificationConfigRepository.findById("notifierId-unknown");
        assertFalse(optNotificationFound.isPresent());
    }

    @Test
    public void shouldFindByHookAndReference() throws Exception {
        List<GenericNotificationConfig> configs = genericNotificationConfigRepository.findByReferenceAndHook(
            "B",
            NotificationReferenceType.APPLICATION,
            "search"
        );

        assertEquals("size", 2, configs.size());
        List<String> userIds = configs.stream().map(GenericNotificationConfig::getNotifier).toList();
        assertTrue("notifierA", userIds.contains("notifierA"));
        assertTrue("notifierB", userIds.contains("notifierB"));
    }

    @Test
    public void shouldDeleteByEmail() throws Exception {
        genericNotificationConfigRepository.deleteByConfig("test@gravitee.io");
        assertFalse(genericNotificationConfigRepository.findById("config-to-delete").isPresent());
    }

    @Test
    public void should_delete_by_reference_type_and_reference_id() throws Exception {
        int nbBeforeDeletion = genericNotificationConfigRepository.findByReference(NotificationReferenceType.PORTAL, "ToBeDeleted").size();
        List<String> deleted = genericNotificationConfigRepository.deleteByReferenceIdAndReferenceType(
            "ToBeDeleted",
            NotificationReferenceType.PORTAL
        );
        int nbAfterDeletion = genericNotificationConfigRepository.findByReference(NotificationReferenceType.PORTAL, "ToBeDeleted").size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted.size());
        assertEquals(0, nbAfterDeletion);
    }
}

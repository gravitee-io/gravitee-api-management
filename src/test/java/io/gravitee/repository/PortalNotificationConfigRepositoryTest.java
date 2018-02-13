/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortalNotificationConfigRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/portalnotificationConfig-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final PortalNotificationConfig cfg = new PortalNotificationConfig();
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-created");
        cfg.setUser("userid");
        cfg.setHooks(Arrays.asList("A", "B", "C"));
        cfg.setUpdatedAt(new Date(1439022010883L));
        cfg.setCreatedAt(new Date(1439022010883L));

        PortalNotificationConfig notificationCreated = portalNotificationConfigRepository.create(cfg);

        assertEquals(cfg.getReferenceType(), notificationCreated.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationCreated.getReferenceId());
        assertEquals(cfg.getUser(), notificationCreated.getUser());
        assertEquals(cfg.getHooks(), notificationCreated.getHooks());
        assertEquals(cfg.getCreatedAt(), notificationCreated.getCreatedAt());
        assertEquals(cfg.getUpdatedAt(), notificationCreated.getUpdatedAt());
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-delete").isPresent());
        final PortalNotificationConfig cfg = new PortalNotificationConfig();
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-to-delete");
        cfg.setUser("userid");
        portalNotificationConfigRepository.delete(cfg);
        assertFalse(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-delete").isPresent());
    }

    @Test
    public void shouldUpdate() throws Exception {
        final PortalNotificationConfig cfg = new PortalNotificationConfig();
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-to-update");
        cfg.setUser("userid");
        cfg.setHooks(Arrays.asList("D", "B", "C"));
        cfg.setUpdatedAt(new Date(1479022010883L));
        cfg.setCreatedAt(new Date(1469022010883L));

        PortalNotificationConfig notificationUpdated = portalNotificationConfigRepository.update(cfg);

        assertEquals(cfg.getReferenceType(), notificationUpdated.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationUpdated.getReferenceId());
        assertEquals(cfg.getUser(), notificationUpdated.getUser());
        assertTrue(cfg.getHooks().containsAll(notificationUpdated.getHooks()));
        assertEquals(cfg.getCreatedAt(), notificationUpdated.getCreatedAt());
        assertEquals(cfg.getUpdatedAt(), notificationUpdated.getUpdatedAt());
    }

    @Test
    public void shouldFindById() throws Exception {
        final PortalNotificationConfig cfg = new PortalNotificationConfig();
        cfg.setReferenceType(NotificationReferenceType.API);
        cfg.setReferenceId("config-to-find");
        cfg.setUser("userid");
        cfg.setHooks(Arrays.asList("A", "B"));
        cfg.setUpdatedAt(new Date(1439022010883L));
        cfg.setCreatedAt(new Date(1439022010883L));

        Optional<PortalNotificationConfig> optNotificationFound =
                portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-find");

        assertTrue(optNotificationFound.isPresent());
        PortalNotificationConfig notificationFound = optNotificationFound.get();
        assertEquals(cfg.getReferenceType(), notificationFound.getReferenceType());
        assertEquals(cfg.getReferenceId(), notificationFound.getReferenceId());
        assertEquals(cfg.getUser(), notificationFound.getUser());
        assertEquals(cfg.getHooks(), notificationFound.getHooks());
        assertEquals(cfg.getCreatedAt(), notificationFound.getCreatedAt());
        assertEquals(cfg.getUpdatedAt(), notificationFound.getUpdatedAt());
    }

    @Test
    public void shouldNotFoundById() throws Exception {
        Optional<PortalNotificationConfig> optNotificationFound;
        //userid
        optNotificationFound = portalNotificationConfigRepository.findById(
                "userid-unknown",
                NotificationReferenceType.API,
                "config-to-find");
        assertFalse(optNotificationFound.isPresent());
        //type
        optNotificationFound = portalNotificationConfigRepository.findById(
                "userid",
                NotificationReferenceType.APPLICATION,
                "config-to-find");
        assertFalse(optNotificationFound.isPresent());
        //ref
        optNotificationFound = portalNotificationConfigRepository.findById(
                "userid",
                NotificationReferenceType.API,
                "config-to-not-find");
        assertFalse(optNotificationFound.isPresent());
    }

    @Test
    public void shouldFindByHookAndReference() throws Exception {
        List<PortalNotificationConfig> configs = portalNotificationConfigRepository.findByReferenceAndHook(
                "B",
                NotificationReferenceType.APPLICATION,
                "search");

        assertEquals("size", 2, configs.size());
        List<String> userIds = configs.stream().map(PortalNotificationConfig::getUser).collect(Collectors.toList());
        assertTrue("userA", userIds.contains("userA"));
        assertTrue("userB", userIds.contains("userB"));
    }
}

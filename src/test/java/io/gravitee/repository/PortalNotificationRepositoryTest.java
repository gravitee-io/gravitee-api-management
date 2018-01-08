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
import io.gravitee.repository.management.model.PortalNotification;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class PortalNotificationRepositoryTest extends AbstractRepositoryTest {

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
        assertEquals(notification.getCreatedAt(), notificationCreated.getCreatedAt());
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
        assertEquals(new Date(1439022010883L), notification.getCreatedAt());
    }

    @Test
    public void shouldNotFindByUsername() throws Exception {
        List<PortalNotification> notifications = portalNotificationRepository.findByUser("unknown");

        assertTrue(notifications.isEmpty());
    }
}

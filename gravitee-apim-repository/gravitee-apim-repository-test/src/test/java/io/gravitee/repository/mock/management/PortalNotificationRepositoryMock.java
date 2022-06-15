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
package io.gravitee.repository.mock.management;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalNotificationRepositoryMock extends AbstractRepositoryMock<PortalNotificationRepository> {

    public PortalNotificationRepositoryMock() {
        super(PortalNotificationRepository.class);
    }

    @Override
    protected void prepare(PortalNotificationRepository portalNotificationRepository) throws Exception {
        // create
        final PortalNotification notificationCreated = new PortalNotification();
        notificationCreated.setId("notif-create");
        notificationCreated.setTitle("notif-title");
        notificationCreated.setMessage("notif-message");
        notificationCreated.setUser("notif-userId");
        notificationCreated.setCreatedAt(new Date(1439022010883L));
        when(portalNotificationRepository.create(any(PortalNotification.class))).thenReturn(notificationCreated);

        //delete
        when(portalNotificationRepository.findByUser(eq("notif-userId-toDelete")))
            .thenReturn(
                singletonList(mock(PortalNotification.class)),
                emptyList(),
                singletonList(mock(PortalNotification.class)),
                emptyList()
            );

        //findByUserId
        final PortalNotification notificationFindByUsername = new PortalNotification();
        notificationFindByUsername.setId("notif-findByUserId");
        notificationFindByUsername.setTitle("notif-title-findByUserId");
        notificationFindByUsername.setMessage("notif-message-findByUserId");
        notificationFindByUsername.setUser("notif-userId-findByUserId");
        notificationFindByUsername.setCreatedAt(new Date(1439022010883L));
        when(portalNotificationRepository.findByUser(eq("notif-userId-findByUserId")))
            .thenReturn(singletonList(notificationFindByUsername));
        when(portalNotificationRepository.findByUser(eq("unknown"))).thenReturn(emptyList());

        final PortalNotification notificationFindById = new PortalNotification();
        notificationFindById.setId("notif-findById");
        notificationFindById.setTitle("notif-title-findById");
        notificationFindById.setMessage("notif-message-findById");
        notificationFindById.setUser("notif-userId-findById");
        notificationFindById.setCreatedAt(new Date(1439022010883L));
        when(portalNotificationRepository.findById(eq("notif-findById"))).thenReturn(Optional.ofNullable(notificationFindById));
    }
}

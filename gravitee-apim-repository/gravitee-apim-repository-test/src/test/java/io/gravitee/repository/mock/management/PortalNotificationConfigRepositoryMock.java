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
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalNotificationConfigRepositoryMock extends AbstractRepositoryMock<PortalNotificationConfigRepository> {

    public PortalNotificationConfigRepositoryMock() {
        super(PortalNotificationConfigRepository.class);
    }

    @Override
    protected void prepare(PortalNotificationConfigRepository portalNotificationConfigRepository) throws Exception {
        //create
        final PortalNotificationConfig createdCfg = new PortalNotificationConfig();
        createdCfg.setReferenceType(NotificationReferenceType.API);
        createdCfg.setReferenceId("config-created");
        createdCfg.setUser("userid");
        createdCfg.setHooks(Arrays.asList("A", "B", "C"));
        createdCfg.setUpdatedAt(new Date(1439022010883L));
        createdCfg.setCreatedAt(new Date(1439022010883L));
        when(portalNotificationConfigRepository.create(any())).thenReturn(createdCfg);

        //update
        final PortalNotificationConfig updatedCfg = new PortalNotificationConfig();
        updatedCfg.setReferenceType(NotificationReferenceType.API);
        updatedCfg.setReferenceId("config-to-update");
        updatedCfg.setUser("userid");
        updatedCfg.setHooks(Arrays.asList("D", "B", "C"));
        updatedCfg.setUpdatedAt(new Date(1479022010883L));
        updatedCfg.setCreatedAt(new Date(1469022010883L));
        when(portalNotificationConfigRepository.update(any())).thenReturn(updatedCfg);

        //delete
        when(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-delete"))
            .thenReturn(of(mock(PortalNotificationConfig.class)), empty());
        when(portalNotificationConfigRepository.findById("useridToDelete", NotificationReferenceType.API, "config")).thenReturn(empty());

        //findById
        final PortalNotificationConfig foundCfg = new PortalNotificationConfig();
        foundCfg.setReferenceType(NotificationReferenceType.API);
        foundCfg.setReferenceId("config-to-find");
        foundCfg.setUser("userid");
        foundCfg.setHooks(Arrays.asList("A", "B"));
        foundCfg.setUpdatedAt(new Date(1439022010883L));
        foundCfg.setCreatedAt(new Date(1439022010883L));
        when(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-find"))
            .thenReturn(of(foundCfg));

        //notFoundById
        when(portalNotificationConfigRepository.findById("userid-unknown", NotificationReferenceType.API, "config-to-find"))
            .thenReturn(empty());
        when(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.APPLICATION, "config-to-find"))
            .thenReturn(empty());
        when(portalNotificationConfigRepository.findById("userid", NotificationReferenceType.API, "config-to-not-find"))
            .thenReturn(empty());

        //findByReferenceAndHook
        PortalNotificationConfig n1 = mock(PortalNotificationConfig.class);
        when(n1.getUser()).thenReturn("userA");
        PortalNotificationConfig n2 = mock(PortalNotificationConfig.class);
        when(n2.getUser()).thenReturn("userB");
        when(portalNotificationConfigRepository.findByReferenceAndHook("B", NotificationReferenceType.APPLICATION, "search"))
            .thenReturn(Arrays.asList(n1, n2));
        when(portalNotificationConfigRepository.findByReferenceAndHook("D", NotificationReferenceType.APPLICATION, "search"))
            .thenReturn(emptyList());

        // shouldDeleteReference
        when(portalNotificationConfigRepository.findById("apiToDelete-1", NotificationReferenceType.API, "apiToDelete"))
            .thenReturn(of(mock(PortalNotificationConfig.class)), empty());
        when(portalNotificationConfigRepository.findById("apiToDelete-2", NotificationReferenceType.API, "apiToDelete"))
            .thenReturn(of(mock(PortalNotificationConfig.class)), empty());
    }
}

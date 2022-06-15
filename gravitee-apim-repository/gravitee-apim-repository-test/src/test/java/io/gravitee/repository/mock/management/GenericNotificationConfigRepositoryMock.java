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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Date;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GenericNotificationConfigRepositoryMock extends AbstractRepositoryMock<GenericNotificationConfigRepository> {

    public GenericNotificationConfigRepositoryMock() {
        super(GenericNotificationConfigRepository.class);
    }

    @Override
    protected void prepare(GenericNotificationConfigRepository genericNotificationConfigRepository) throws Exception {
        //create
        final GenericNotificationConfig createdCfg = new GenericNotificationConfig();
        createdCfg.setId("new-id");
        createdCfg.setName("new config");
        createdCfg.setReferenceType(NotificationReferenceType.API);
        createdCfg.setReferenceId("config-created");
        createdCfg.setNotifier("notifierId");
        createdCfg.setConfig("my new configuration");
        createdCfg.setUseSystemProxy(true);
        createdCfg.setHooks(Arrays.asList("A", "B", "C"));
        createdCfg.setUpdatedAt(new Date(1439022010883L));
        createdCfg.setCreatedAt(new Date(1439022010883L));
        when(genericNotificationConfigRepository.create(any())).thenReturn(createdCfg);

        //update
        final GenericNotificationConfig updatedCfg = new GenericNotificationConfig();
        updatedCfg.setId("notif-to-update");
        updatedCfg.setName("notif-updated");
        updatedCfg.setReferenceType(NotificationReferenceType.API);
        updatedCfg.setReferenceId("config-to-update");
        updatedCfg.setNotifier("notifierId");
        updatedCfg.setConfig("updated configuration");
        updatedCfg.setUseSystemProxy(true);
        updatedCfg.setHooks(Arrays.asList("D", "B", "C"));
        updatedCfg.setUpdatedAt(new Date(1479022010883L));
        updatedCfg.setCreatedAt(new Date(1469022010883L));
        when(genericNotificationConfigRepository.update(any())).thenReturn(updatedCfg);

        //delete
        when(genericNotificationConfigRepository.findById("notif-to-delete"))
            .thenReturn(of(mock(GenericNotificationConfig.class)), empty());
        when(genericNotificationConfigRepository.findById("config-to-delete")).thenReturn(empty());

        //findById
        final GenericNotificationConfig foundCfg = new GenericNotificationConfig();
        foundCfg.setId("notif-to-find");
        foundCfg.setName("notif-to-find");
        foundCfg.setReferenceType(NotificationReferenceType.API);
        foundCfg.setReferenceId("config-to-find");
        foundCfg.setNotifier("notifierId");
        foundCfg.setConfig("my config");
        foundCfg.setUseSystemProxy(true);
        foundCfg.setHooks(Arrays.asList("A", "B"));
        foundCfg.setUpdatedAt(new Date(1439022010883L));
        foundCfg.setCreatedAt(new Date(1439022010883L));
        when(genericNotificationConfigRepository.findById("notif-to-find")).thenReturn(of(foundCfg));

        //notFoundById
        when(genericNotificationConfigRepository.findById("notifierId-unknown")).thenReturn(empty());

        //findByReferenceAndHook
        GenericNotificationConfig n1 = mock(GenericNotificationConfig.class);
        when(n1.getNotifier()).thenReturn("notifierA");
        GenericNotificationConfig n2 = mock(GenericNotificationConfig.class);
        when(n2.getNotifier()).thenReturn("notifierB");
        when(genericNotificationConfigRepository.findByReferenceAndHook("B", NotificationReferenceType.APPLICATION, "search"))
            .thenReturn(Arrays.asList(n1, n2));
    }
}

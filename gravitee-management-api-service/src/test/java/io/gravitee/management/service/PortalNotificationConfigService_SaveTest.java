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
package io.gravitee.management.service;

import io.gravitee.management.model.notification.PortalNotificationConfigEntity;
import io.gravitee.management.service.impl.PortalNotificationConfigServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalNotificationConfigService_SaveTest {

    @InjectMocks
    private PortalNotificationConfigService portalNotificationConfigService = new PortalNotificationConfigServiceImpl();

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Test
    public void shouldDelete() throws TechnicalException {
        PortalNotificationConfigEntity cfgEntity = mock(PortalNotificationConfigEntity.class);
        when(cfgEntity.getReferenceType()).thenReturn(NotificationReferenceType.API.name());
        when(cfgEntity.getReferenceId()).thenReturn("123");
        when(cfgEntity.getUser()).thenReturn("user");
        when(cfgEntity.getHooks()).thenReturn(Collections.emptyList());

        final PortalNotificationConfigEntity entity = portalNotificationConfigService.save(cfgEntity);

        assertNotNull(entity);
        assertEquals("referenceId", cfgEntity.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfgEntity.getReferenceType(), entity.getReferenceType());
        assertEquals("userId", cfgEntity.getUser(), entity.getUser());
        assertEquals("hooks", cfgEntity.getHooks(), entity.getHooks());
        verify(portalNotificationConfigRepository, never()).findById(any(), any(), any());
        verify(portalNotificationConfigRepository, times(1)).delete(any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {

        PortalNotificationConfig cfg = mock(PortalNotificationConfig.class);
        when(cfg.getReferenceType()).thenReturn(NotificationReferenceType.API);
        when(cfg.getReferenceId()).thenReturn("123");
        when(cfg.getUser()).thenReturn("user");
        when(cfg.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));

        PortalNotificationConfigEntity cfgEntity = mock(PortalNotificationConfigEntity.class);
        when(cfgEntity.getReferenceType()).thenReturn(NotificationReferenceType.API.name());
        when(cfgEntity.getReferenceId()).thenReturn("123");
        when(cfgEntity.getUser()).thenReturn("user");
        when(cfgEntity.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));

        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).
                thenReturn(of(cfg));
        when(portalNotificationConfigRepository.update(any(PortalNotificationConfig.class))).thenReturn(cfg);

        final PortalNotificationConfigEntity entity = portalNotificationConfigService.save(cfgEntity);

        assertNotNull(entity);
        assertEquals("referenceId", cfgEntity.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfgEntity.getReferenceType(), entity.getReferenceType());
        assertEquals("user", cfgEntity.getUser(), entity.getUser());
        assertEquals("hooks", cfgEntity.getHooks(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
        verify(portalNotificationConfigRepository, times(1)).update(any());
        verify(portalNotificationConfigRepository, never()).delete(any());
        verify(portalNotificationConfigRepository, never()).create(any());
    }


    @Test
    public void shouldCreate() throws TechnicalException {

        PortalNotificationConfig cfg = mock(PortalNotificationConfig.class);
        when(cfg.getReferenceType()).thenReturn(NotificationReferenceType.API);
        when(cfg.getReferenceId()).thenReturn("123");
        when(cfg.getUser()).thenReturn("user");
        when(cfg.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));

        PortalNotificationConfigEntity cfgEntity = mock(PortalNotificationConfigEntity.class);
        when(cfgEntity.getReferenceType()).thenReturn(NotificationReferenceType.API.name());
        when(cfgEntity.getReferenceId()).thenReturn("123");
        when(cfgEntity.getUser()).thenReturn("user");
        when(cfgEntity.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));

        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).
                thenReturn(empty());
        when(portalNotificationConfigRepository.create(any(PortalNotificationConfig.class))).thenReturn(cfg);

        final PortalNotificationConfigEntity entity = portalNotificationConfigService.save(cfgEntity);

        assertNotNull(entity);
        assertEquals("referenceId", cfgEntity.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfgEntity.getReferenceType(), entity.getReferenceType());
        assertEquals("user", cfgEntity.getUser(), entity.getUser());
        assertEquals("hooks", cfgEntity.getHooks(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
        verify(portalNotificationConfigRepository, times(1)).create(any());
        verify(portalNotificationConfigRepository, never()).delete(any());
        verify(portalNotificationConfigRepository, never()).update(any());
    }

}

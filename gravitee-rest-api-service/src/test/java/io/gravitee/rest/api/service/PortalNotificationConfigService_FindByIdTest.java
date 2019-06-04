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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.impl.PortalNotificationConfigServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
public class PortalNotificationConfigService_FindByIdTest {

    @InjectMocks
    private PortalNotificationConfigService portalNotificationConfigService = new PortalNotificationConfigServiceImpl();

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Test
    public void shouldFindExistingConfig() throws TechnicalException {
        PortalNotificationConfig cfg = mock(PortalNotificationConfig.class);
        when(cfg.getReferenceType()).thenReturn(NotificationReferenceType.API);
        when(cfg.getReferenceId()).thenReturn("123");
        when(cfg.getUser()).thenReturn("user");
        when(cfg.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));
        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).
                thenReturn(of(cfg));

        final PortalNotificationConfigEntity entity = portalNotificationConfigService.findById("user", NotificationReferenceType.API, "123");

        assertNotNull(entity);
        assertEquals("referenceId", cfg.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfg.getReferenceType().name(), entity.getReferenceType());
        assertEquals("user", cfg.getUser(), entity.getUser());
        assertEquals("hooks", cfg.getHooks(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
    }

    @Test
    public void shouldNotFindConfig() throws TechnicalException {
        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).
                thenReturn(empty());

        final PortalNotificationConfigEntity entity = portalNotificationConfigService.findById("user", NotificationReferenceType.API, "123");

        assertNotNull(entity);
        assertEquals("referenceId", "123", entity.getReferenceId());
        assertEquals("referenceType", NotificationReferenceType.API.name(), entity.getReferenceType());
        assertEquals("user", "user", entity.getUser());
        assertEquals("hooks", Collections.emptyList(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
    }
}

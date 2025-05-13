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
package io.gravitee.rest.api.service.impl;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalNotificationConfigService_SaveTest {

    @Mock
    PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Mock
    MembershipService membershipService;

    PortalNotificationConfigService underTest;

    @Before
    public void setup() {
        underTest = new PortalNotificationConfigServiceImpl(portalNotificationConfigRepository, membershipService);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        PortalNotificationConfigEntity cfgEntity = mock(PortalNotificationConfigEntity.class);
        when(cfgEntity.getReferenceType()).thenReturn(NotificationReferenceType.API.name());
        when(cfgEntity.getReferenceId()).thenReturn("123");
        when(cfgEntity.getUser()).thenReturn("user");
        when(cfgEntity.getHooks()).thenReturn(Collections.emptyList());

        final PortalNotificationConfigEntity entity = underTest.save(cfgEntity);

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

        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).thenReturn(of(cfg));
        when(portalNotificationConfigRepository.update(any(PortalNotificationConfig.class))).thenReturn(cfg);

        final PortalNotificationConfigEntity entity = underTest.save(cfgEntity);

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

        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).thenReturn(empty());
        when(portalNotificationConfigRepository.create(any(PortalNotificationConfig.class))).thenReturn(cfg);

        final PortalNotificationConfigEntity entity = underTest.save(cfgEntity);

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

    @Test
    public void shouldRemoveGroupIds() throws TechnicalException {
        PortalNotificationConfig config = PortalNotificationConfig
            .builder()
            .groups(new HashSet<>(Set.of("1", "2", "3")))
            .user("po")
            .referenceType(NotificationReferenceType.API)
            .referenceId("123")
            .build();

        when(membershipService.getPrimaryOwnerUserId(any(), eq(MembershipReferenceType.API), eq("123"))).thenReturn("po");
        when(portalNotificationConfigRepository.findById("po", NotificationReferenceType.API, "123")).thenReturn(Optional.of(config));

        underTest.removeGroupIds("123", Set.of("3"));
        verify(portalNotificationConfigRepository, times(1))
            .update(
                assertArg(c -> {
                    assertEquals(Set.of("1", "2"), c.getGroups());
                })
            );
    }
}

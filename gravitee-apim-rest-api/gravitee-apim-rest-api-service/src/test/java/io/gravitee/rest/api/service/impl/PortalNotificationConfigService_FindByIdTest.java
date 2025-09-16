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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalNotificationConfigService_FindByIdTest {

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Mock
    GroupService groupService;

    @Mock
    MembershipService membershipService;

    private PortalNotificationConfigServiceImpl underTest;

    @Before
    public void setup() {
        underTest = new PortalNotificationConfigServiceImpl(portalNotificationConfigRepository, membershipService, groupService);
    }

    @Test
    public void shouldFindExistingConfig() throws TechnicalException {
        PortalNotificationConfig cfg = mock(PortalNotificationConfig.class);
        when(cfg.getReferenceType()).thenReturn(NotificationReferenceType.API);
        when(cfg.getReferenceId()).thenReturn("123");
        when(cfg.getUser()).thenReturn("user");
        when(cfg.getHooks()).thenReturn(Arrays.asList("A", "B", "C"));
        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).thenReturn(of(cfg));

        final PortalNotificationConfigEntity entity = underTest.findById("user", NotificationReferenceType.API, "123");

        assertNotNull(entity);
        assertEquals("referenceId", cfg.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfg.getReferenceType().name(), entity.getReferenceType());
        assertEquals("user", cfg.getUser(), entity.getUser());
        assertEquals("hooks", cfg.getHooks(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
    }

    @Test
    public void shouldAddGroupHooks() throws TechnicalException {
        var cfg = PortalNotificationConfig.builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("123")
            .user("user")
            .groups(Set.of())
            .build();

        var poCfg = PortalNotificationConfig.builder()
            .referenceType(NotificationReferenceType.API)
            .referenceId("123")
            .user("api-po-id")
            .hooks(List.of("A", "B", "C"))
            .groups(Set.of("group-id"))
            .build();

        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).thenReturn(of(cfg));

        when(groupService.findByUser("user")).thenReturn(Set.of(GroupEntity.builder().id("group-id").build()));

        when(membershipService.getPrimaryOwnerUserId(anyString(), eq(MembershipReferenceType.API), eq("123"))).thenReturn("api-po-id");

        when(portalNotificationConfigRepository.findById("api-po-id", NotificationReferenceType.API, "123")).thenReturn(of(poCfg));

        var entity = underTest.findById("user", NotificationReferenceType.API, "123");

        assertNotNull(entity);
        assertEquals("referenceId", cfg.getReferenceId(), entity.getReferenceId());
        assertEquals("referenceType", cfg.getReferenceType().name(), entity.getReferenceType());
        assertEquals("user", cfg.getUser(), entity.getUser());
        assertEquals("hooks", cfg.getHooks(), entity.getHooks());
        assertNotNull("groupHooks", entity.getGroupHooks());
        assertEquals("groupHooks", Set.of("A", "B", "C"), entity.getGroupHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
    }

    @Test
    public void shouldNotFindConfig() throws TechnicalException {
        when(portalNotificationConfigRepository.findById("user", NotificationReferenceType.API, "123")).thenReturn(empty());

        final PortalNotificationConfigEntity entity = underTest.findById("user", NotificationReferenceType.API, "123");

        assertNotNull(entity);
        assertEquals("referenceId", "123", entity.getReferenceId());
        assertEquals("referenceType", NotificationReferenceType.API.name(), entity.getReferenceType());
        assertEquals("user", "user", entity.getUser());
        assertEquals("hooks", Collections.emptyList(), entity.getHooks());
        verify(portalNotificationConfigRepository, times(1)).findById("user", NotificationReferenceType.API, "123");
    }
}

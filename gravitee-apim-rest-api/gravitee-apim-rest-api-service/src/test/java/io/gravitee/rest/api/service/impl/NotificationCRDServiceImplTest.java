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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.NotificationCRDDomainService;
import io.gravitee.apim.infra.domain_service.notification.NotificationCRDDomainServiceImpl;
import io.gravitee.definition.model.Origin;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationCRDServiceImplTest {

    private static final String USER_ID = "user-id";
    private static final String API_ID = "api-id";

    NotificationCRDDomainService underTest;

    PortalNotificationConfigService portalNotificationService = mock(PortalNotificationConfigService.class);

    @BeforeEach
    void setUp() {
        underTest = new NotificationCRDDomainServiceImpl(portalNotificationService);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_not_save_notification() {
        when(portalNotificationService.findById(USER_ID, NotificationReferenceType.API, API_ID)).thenReturn(
            PortalNotificationConfigEntity.newDefaultEmpty(USER_ID, NotificationReferenceType.API.name(), API_ID, "org1")
        );
        underTest.syncApiPortalNotifications(API_ID, USER_ID, null);
        verify(portalNotificationService, never()).save(any());
    }

    @Test
    void should_save_notification() {
        PortalNotificationConfigEntity notification = PortalNotificationConfigEntity.newDefaultEmpty(
            USER_ID,
            NotificationReferenceType.API.name(),
            API_ID,
            "org1"
        );
        notification.setHooks(List.of("hook1", "hook2"));

        underTest.syncApiPortalNotifications(API_ID, USER_ID, notification);
        verify(portalNotificationService, atMostOnce()).save(notification);
    }

    @Test
    void should_delete_notification() {
        PortalNotificationConfigEntity notification = PortalNotificationConfigEntity.newDefaultEmpty(
            USER_ID,
            NotificationReferenceType.API.name(),
            API_ID,
            "org1"
        );
        notification.setHooks(List.of("hook1", "hook2"));
        when(portalNotificationService.findById(USER_ID, NotificationReferenceType.API, API_ID)).thenReturn(notification);
        underTest.syncApiPortalNotifications(API_ID, USER_ID, null);

        // saving default/empty object triggers deletion
        verify(portalNotificationService, atMostOnce()).save(
            PortalNotificationConfigEntity.newDefaultEmpty(USER_ID, NotificationReferenceType.API.name(), API_ID, "org1")
        );
    }

    @Test
    void should_clear_notification_when_events_are_empty() throws Exception {
        GraviteeContext.setCurrentOrganization("org-id");
        var repository = mock(PortalNotificationConfigRepository.class);
        var service = new NotificationCRDDomainServiceImpl(
            new PortalNotificationConfigServiceImpl(repository, mock(MembershipService.class), mock(GroupService.class))
        );
        var notification = PortalNotificationConfigEntity.builder()
            .configType(NotificationConfigType.PORTAL)
            .origin(Origin.KUBERNETES)
            .hooks(List.of())
            .groups(List.of("group-id"))
            .build();

        assertDoesNotThrow(() -> service.syncApiPortalNotifications(API_ID, USER_ID, notification));

        verify(repository).delete(assertArg(config -> assertThat(config.getOrganizationId()).isEqualTo("org-id")));
    }

    @Test
    void should_save_notification_with_current_organization() {
        GraviteeContext.setCurrentOrganization("org-id");
        var notification = PortalNotificationConfigEntity.builder()
            .configType(NotificationConfigType.PORTAL)
            .origin(Origin.KUBERNETES)
            .hooks(List.of("API_STARTED"))
            .build();

        underTest.syncApiPortalNotifications(API_ID, USER_ID, notification);

        verify(portalNotificationService).save(assertArg(saved -> assertThat(saved.getOrganizationId()).isEqualTo("org-id")));
    }
}

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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalNotificationConfigUpgraderTest {

    private static final ZonedDateTime CREATED_AT = Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_AT = Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault());

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    private PortalNotificationConfigUpgrader upgrader;

    @Before
    public void setUp() throws Exception {
        upgrader = new PortalNotificationConfigUpgrader(environmentRepository, portalNotificationConfigRepository);
    }

    @Test
    public void should_upgrade_only_portal_default_notification() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("env#1").build(), Environment.builder().id("env#2").build()));

        Set<PortalNotificationConfig> portalNotificationConfigs = Set.of(
            aPortalNotificationConfig("DEFAULT", NotificationReferenceType.PORTAL, "user#1"),
            aPortalNotificationConfig("DEFAULT", NotificationReferenceType.PORTAL, "user#2"),
            aPortalNotificationConfig("api#1", NotificationReferenceType.API, "user#1"),
            aPortalNotificationConfig("app#1", NotificationReferenceType.APPLICATION, "user#1")
        );
        when(portalNotificationConfigRepository.findAll()).thenReturn(portalNotificationConfigs);

        assertTrue(upgrader.upgrade());

        verify(portalNotificationConfigRepository, times(4)).create(any());
        verify(portalNotificationConfigRepository)
            .create(
                argThat(portalNotificationConfig ->
                    portalNotificationConfig.equals(aPortalNotificationConfig("env#1", NotificationReferenceType.ENVIRONMENT, "user#1"))
                )
            );
        verify(portalNotificationConfigRepository)
            .create(
                argThat(portalNotificationConfig ->
                    portalNotificationConfig.equals(aPortalNotificationConfig("env#2", NotificationReferenceType.ENVIRONMENT, "user#1"))
                )
            );
        verify(portalNotificationConfigRepository)
            .create(
                argThat(portalNotificationConfig ->
                    portalNotificationConfig.equals(aPortalNotificationConfig("env#1", NotificationReferenceType.ENVIRONMENT, "user#2"))
                )
            );
        verify(portalNotificationConfigRepository)
            .create(
                argThat(portalNotificationConfig ->
                    portalNotificationConfig.equals(aPortalNotificationConfig("env#2", NotificationReferenceType.ENVIRONMENT, "user#2"))
                )
            );

        verify(portalNotificationConfigRepository, times(2)).delete(any());
        verify(portalNotificationConfigRepository).delete(aPortalNotificationConfig("DEFAULT", NotificationReferenceType.PORTAL, "user#1"));
        verify(portalNotificationConfigRepository).delete(aPortalNotificationConfig("DEFAULT", NotificationReferenceType.PORTAL, "user#2"));
    }

    @Test
    public void should_not_create_if_not_find_default_notification() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        when(portalNotificationConfigRepository.findAll()).thenReturn(Collections.emptySet());

        assertTrue(upgrader.upgrade());

        verify(portalNotificationConfigRepository, never()).create(any());
        verify(portalNotificationConfigRepository, never()).delete(any());
    }

    private PortalNotificationConfig aPortalNotificationConfig(String referenceId, NotificationReferenceType referenceType, String user) {
        return aPortalNotificationConfig(referenceId, referenceType, user, Collections.emptyList());
    }

    private PortalNotificationConfig aPortalNotificationConfig(
        String referenceId,
        NotificationReferenceType referenceType,
        String user,
        List<String> hooks
    ) {
        return PortalNotificationConfig
            .builder()
            .referenceId(referenceId)
            .referenceType(referenceType)
            .user(user)
            .hooks(hooks)
            .createdAt(Date.from(CREATED_AT.toInstant()))
            .updatedAt(Date.from(UPDATED_AT.toInstant()))
            .build();
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.PORTAL_NOTIFICATION_CONFIG_UPGRADER, upgrader.getOrder());
    }
}

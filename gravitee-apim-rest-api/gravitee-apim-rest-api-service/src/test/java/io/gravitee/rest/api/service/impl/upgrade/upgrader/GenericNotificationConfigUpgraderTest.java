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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.service.common.UuidString;
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
public class GenericNotificationConfigUpgraderTest {

    private static final ZonedDateTime CREATED_AT = Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_AT = Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault());

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    private GenericNotificationConfigUpgrader upgrader;

    @Before
    public void setUp() throws Exception {
        upgrader = new GenericNotificationConfigUpgrader(environmentRepository, genericNotificationConfigRepository);
    }

    @Test
    public void should_upgrade_only_portal_default_notification() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("env#1").build(), Environment.builder().id("env#2").build()));

        Set<GenericNotificationConfig> genericNotificationConfigs = Set.of(
            aGenericNotificationConfig("DEFAULT", NotificationReferenceType.PORTAL),
            aGenericNotificationConfig("api#1", NotificationReferenceType.API),
            aGenericNotificationConfig("app#1", NotificationReferenceType.APPLICATION)
        );
        when(genericNotificationConfigRepository.findAll()).thenReturn(genericNotificationConfigs);

        assertTrue(upgrader.upgrade());

        verify(genericNotificationConfigRepository, times(2)).create(any());
        verify(genericNotificationConfigRepository)
            .create(
                argThat(notification ->
                    notification.getReferenceId().equals("env#1") &&
                    notification.getReferenceType().equals(NotificationReferenceType.ENVIRONMENT)
                )
            );
        verify(genericNotificationConfigRepository)
            .create(
                argThat(notification ->
                    notification.getReferenceId().equals("env#2") &&
                    notification.getReferenceType().equals(NotificationReferenceType.ENVIRONMENT)
                )
            );

        verify(genericNotificationConfigRepository, times(1)).delete(any());
    }

    @Test
    public void should_not_create_if_not_find_generic_notification() throws TechnicalException {
        when(environmentRepository.findAll())
            .thenReturn(Set.of(Environment.builder().id("DEFAULT").build(), Environment.builder().id("env#1").build()));

        when(genericNotificationConfigRepository.findAll()).thenReturn(Collections.emptySet());

        assertTrue(upgrader.upgrade());

        verify(genericNotificationConfigRepository, never()).create(any());
        verify(genericNotificationConfigRepository, never()).delete(any());
    }

    private GenericNotificationConfig aGenericNotificationConfig(String referenceId, NotificationReferenceType referenceType) {
        return aGenericNotificationConfig(referenceId, referenceType, Collections.emptyList());
    }

    private GenericNotificationConfig aGenericNotificationConfig(
        String referenceId,
        NotificationReferenceType referenceType,
        List<String> hooks
    ) {
        return GenericNotificationConfig
            .builder()
            .id(UuidString.generateRandom())
            .referenceId(referenceId)
            .referenceType(referenceType)
            .hooks(hooks)
            .createdAt(Date.from(CREATED_AT.toInstant()))
            .updatedAt(Date.from(UPDATED_AT.toInstant()))
            .build();
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.GENERIC_NOTIFICATION_CONFIG_UPGRADER, upgrader.getOrder());
    }
}

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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GenericNotificationConfigServiceImplTest {

    @InjectMocks
    private GenericNotificationConfigServiceImpl service;

    @Test
    public void convert_fromBasicToEntity() {
        GenericNotificationConfig notifConfig = GenericNotificationConfig
            .builder()
            .id("nc1")
            .name("notif 1")
            .referenceType(NotificationReferenceType.ENVIRONMENT)
            .referenceId("env1")
            .notifier("notifier1")
            .config("config1")
            .useSystemProxy(true)
            .hooks(List.of("USER_REGISTRATION_REQUEST", "USER_REGISTERED"))
            .organizationId("org1")
            .build();
        GenericNotificationConfigEntity notifConfigEntity = service.convert(notifConfig);
        assertAll(
            () -> assertThat(notifConfigEntity.getId()).isEqualTo("nc1"),
            () -> assertThat(notifConfigEntity.getName()).isEqualTo("notif 1"),
            () -> assertThat(notifConfigEntity.getReferenceType()).isEqualTo(NotificationReferenceType.ENVIRONMENT.name()),
            () -> assertThat(notifConfigEntity.getReferenceId()).isEqualTo("env1"),
            () -> assertThat(notifConfigEntity.getNotifier()).isEqualTo("notifier1"),
            () -> assertThat(notifConfigEntity.getConfig()).isEqualTo("config1"),
            () -> assertTrue(notifConfigEntity.isUseSystemProxy()),
            () -> assertThat(notifConfigEntity.getHooks().size()).isEqualTo(2),
            () -> assertThat(notifConfigEntity.getHooks()).containsAll(List.of("USER_REGISTRATION_REQUEST", "USER_REGISTERED")),
            () -> assertThat(notifConfigEntity.getOrganizationId()).isEqualTo("org1")
        );
    }

    @Test
    public void convert_fromEntityToBasic() {
        GenericNotificationConfigEntity notifConfigEntity = GenericNotificationConfigEntity
            .builder()
            .id("nc1")
            .name("notif 1")
            .referenceType("ENVIRONMENT")
            .referenceId("env1")
            .notifier("notifier1")
            .config("config1")
            .useSystemProxy(true)
            .hooks(List.of("USER_REGISTRATION_REQUEST", "USER_REGISTERED"))
            .organizationId("org1")
            .build();
        GenericNotificationConfig notifConfig = service.convert(notifConfigEntity);
        assertAll(
            () -> assertThat(notifConfig.getId()).isEqualTo("nc1"),
            () -> assertThat(notifConfig.getName()).isEqualTo("notif 1"),
            () -> assertThat(notifConfig.getReferenceType()).isEqualTo(NotificationReferenceType.ENVIRONMENT),
            () -> assertThat(notifConfig.getReferenceId()).isEqualTo("env1"),
            () -> assertThat(notifConfig.getNotifier()).isEqualTo("notifier1"),
            () -> assertThat(notifConfig.getConfig()).isEqualTo("config1"),
            () -> assertTrue(notifConfig.isUseSystemProxy()),
            () -> assertThat(notifConfig.getHooks().size()).isEqualTo(2),
            () -> assertThat(notifConfig.getHooks()).containsAll(List.of("USER_REGISTRATION_REQUEST", "USER_REGISTERED")),
            () -> assertThat(notifConfig.getOrganizationId()).isEqualTo("org1")
        );
    }
}

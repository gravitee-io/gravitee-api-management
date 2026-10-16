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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.service.exceptions.NotificationConfigNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class GenericNotificationConfigServiceImplTest {

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @InjectMocks
    private GenericNotificationConfigServiceImpl service;

    @Test
    public void convert_fromBasicToEntity() {
        GenericNotificationConfig notifConfig = GenericNotificationConfig.builder()
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
        GenericNotificationConfigEntity notifConfigEntity = GenericNotificationConfigEntity.builder()
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

    @Test
    public void update_shouldThrowWhenStoredConfigBelongsToAnotherReference() throws Exception {
        GenericNotificationConfig stored = GenericNotificationConfig.builder()
            .id("nc1")
            .name("victim")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api-A")
            .notifier("notifier1")
            .organizationId("orgA")
            .createdAt(new Date())
            .build();
        when(genericNotificationConfigRepository.findById("nc1")).thenReturn(Optional.of(stored));

        GenericNotificationConfigEntity entity = GenericNotificationConfigEntity.builder()
            .id("nc1")
            .name("hijacked")
            .referenceType("API")
            .referenceId("api-B")
            .notifier("notifier1")
            .build();

        assertThrows(NotificationConfigNotFoundException.class, () -> service.update(entity));
        verify(genericNotificationConfigRepository, never()).update(any());
    }

    @Test
    public void update_shouldKeepReferenceAndOrganizationFromStoredRecord() throws Exception {
        Date created = new Date(1000L);
        GenericNotificationConfig stored = GenericNotificationConfig.builder()
            .id("nc1")
            .name("old")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api-A")
            .notifier("notifier1")
            .organizationId("orgA")
            .createdAt(created)
            .build();
        when(genericNotificationConfigRepository.findById("nc1")).thenReturn(Optional.of(stored));
        when(genericNotificationConfigRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GenericNotificationConfigEntity entity = GenericNotificationConfigEntity.builder()
            .id("nc1")
            .name("renamed")
            .referenceType("API")
            .referenceId("api-A")
            .notifier("notifier2")
            .config("new-config")
            .organizationId("evil-org")
            .build();

        service.update(entity);

        ArgumentCaptor<GenericNotificationConfig> captor = ArgumentCaptor.forClass(GenericNotificationConfig.class);
        verify(genericNotificationConfigRepository).update(captor.capture());
        GenericNotificationConfig saved = captor.getValue();
        assertAll(
            () -> assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.API),
            () -> assertThat(saved.getReferenceId()).isEqualTo("api-A"),
            () -> assertThat(saved.getOrganizationId()).isEqualTo("orgA"),
            () -> assertThat(saved.getName()).isEqualTo("renamed"),
            () -> assertThat(saved.getConfig()).isEqualTo("new-config"),
            () -> assertThat(saved.getCreatedAt()).isEqualTo(created)
        );
    }

    @Test
    public void delete_shouldThrowWhenConfigBelongsToAnotherReference() throws Exception {
        GenericNotificationConfig stored = GenericNotificationConfig.builder()
            .id("nc1")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api-A")
            .build();
        when(genericNotificationConfigRepository.findById("nc1")).thenReturn(Optional.of(stored));

        assertThrows(NotificationConfigNotFoundException.class, () -> service.delete("nc1", NotificationReferenceType.API, "api-B"));
        verify(genericNotificationConfigRepository, never()).delete(any());
    }

    @Test
    public void delete_shouldDeleteWhenReferenceMatches() throws Exception {
        GenericNotificationConfig stored = GenericNotificationConfig.builder()
            .id("nc1")
            .referenceType(NotificationReferenceType.API)
            .referenceId("api-A")
            .build();
        when(genericNotificationConfigRepository.findById("nc1")).thenReturn(Optional.of(stored));

        service.delete("nc1", NotificationReferenceType.API, "api-A");

        verify(genericNotificationConfigRepository).delete("nc1");
    }
}

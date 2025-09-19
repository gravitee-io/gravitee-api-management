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
package io.gravitee.apim.infra.crud_service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationConfigCrudServiceImplTest {

    GenericNotificationConfigRepository notificationConfigRepository;

    NotificationConfigCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationConfigRepository = mock(GenericNotificationConfigRepository.class);

        service = new NotificationConfigCrudServiceImpl(notificationConfigRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_config() {
            var config = aNotificationConfig();
            service.create(config);

            var captor = ArgumentCaptor.forClass(GenericNotificationConfig.class);
            verify(notificationConfigRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.GenericNotificationConfig.builder()
                        .id("config-id")
                        .name("Default Mail Notifications")
                        .notifier("default-email")
                        .config("${(api.primaryOwner.email)!''}")
                        .hooks(List.of("hook1"))
                        .referenceType(NotificationReferenceType.API)
                        .referenceId("api-id")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_created_configuration() {
            when(notificationConfigRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toCreate = aNotificationConfig();
            var result = service.create(toCreate);

            assertThat(result).isEqualTo(toCreate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(notificationConfigRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(aNotificationConfig()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the API notification config of api-id");
        }
    }

    private static NotificationConfig aNotificationConfig() {
        return NotificationConfig.defaultMailNotificationConfigFor("api-id")
            .toBuilder()
            .id("config-id")
            .hooks(List.of("hook1"))
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .build();
    }
}

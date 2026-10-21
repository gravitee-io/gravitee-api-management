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
package io.gravitee.apim.infra.crud_service.api_key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiKeyFixtures;
import io.gravitee.apim.core.api_key.exception.ApiKeyIdAlreadyExistsException;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ApiKeyCrudServiceImplTest {

    ApiKeyRepository apiKeyRepository;

    ApiKeyCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);

        service = new ApiKeyCrudServiceImpl(apiKeyRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_an_api_key() {
            var apiKey = ApiKeyFixtures.anApiKey();
            service.create(apiKey);

            var captor = ArgumentCaptor.forClass(ApiKey.class);
            verify(apiKeyRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    ApiKey.builder()
                        .id("api-key-id")
                        .subscriptions(apiKey.getSubscriptions())
                        .key("c080f684-2c35-40a1-903c-627c219e0567")
                        .application("application-id")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .expireAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")))
                        .revokedAt(null)
                        .revoked(false)
                        .paused(false)
                        .daysToExpirationOnLastNotification(310)
                        .build()
                );

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(ApiKeyAdapter.INSTANCE.fromEntity(apiKey));
        }

        @Test
        @SneakyThrows
        void should_return_the_created_api_key() {
            when(apiKeyRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toCreate = ApiKeyFixtures.anApiKey();
            var result = service.create(toCreate);

            assertThat(result).isEqualTo(toCreate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(apiKeyRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(ApiKeyFixtures.anApiKey()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to create the api key: api-key-id");
        }

        @Test
        void should_report_an_existing_id_apart_from_other_errors() throws TechnicalException {
            when(apiKeyRepository.create(any())).thenThrow(new DuplicateKeyException("duplicate id"));

            Throwable throwable = catchThrowable(() -> service.create(ApiKeyFixtures.anApiKey()));

            assertThat(throwable).isInstanceOf(ApiKeyIdAlreadyExistsException.class).hasMessageContaining("api-key-id");
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_an_existing_api_key() {
            var apiKey = ApiKeyFixtures.anApiKey();
            service.update(apiKey);

            var captor = ArgumentCaptor.forClass(ApiKey.class);
            verify(apiKeyRepository).update(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    ApiKey.builder()
                        .id("api-key-id")
                        .subscriptions(apiKey.getSubscriptions())
                        .key("c080f684-2c35-40a1-903c-627c219e0567")
                        .application("application-id")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .expireAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")))
                        .revokedAt(null)
                        .revoked(false)
                        .paused(false)
                        .daysToExpirationOnLastNotification(310)
                        .build()
                );

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(ApiKeyAdapter.INSTANCE.fromEntity(apiKey));
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_api_key() {
            when(apiKeyRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = ApiKeyFixtures.anApiKey();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(apiKeyRepository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(ApiKeyFixtures.anApiKey()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to update the api key: api-key-id");
        }
    }

    @Nested
    class AddSubscription {

        @Test
        @SneakyThrows
        void should_attach_the_subscription_with_the_atomic_repository_operation_and_return_the_stored_key() {
            var stored = ApiKeyFixtures.anApiKey().toBuilder().subscriptions(List.of("subscription-id", "new-subscription")).build();
            when(apiKeyRepository.addSubscription("api-key-id", "new-subscription")).thenReturn(
                Optional.of(ApiKeyAdapter.INSTANCE.fromEntity(stored))
            );

            var result = service.addSubscription("api-key-id", "new-subscription");

            assertThat(result).contains(stored);
            verify(apiKeyRepository, never()).update(any());
        }

        @Test
        @SneakyThrows
        void should_return_empty_when_the_key_does_not_exist() {
            when(apiKeyRepository.addSubscription("unknown", "new-subscription")).thenReturn(Optional.empty());

            assertThat(service.addSubscription("unknown", "new-subscription")).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(apiKeyRepository.addSubscription(any(), any())).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.addSubscription("api-key-id", "new-subscription"));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to add subscription new-subscription to the api key: api-key-id");
        }
    }
}

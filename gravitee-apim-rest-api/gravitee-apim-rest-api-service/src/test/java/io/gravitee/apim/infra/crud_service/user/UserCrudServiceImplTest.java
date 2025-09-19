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
package io.gravitee.apim.infra.crud_service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UserCrudServiceImplTest {

    UserRepository userRepository;

    UserCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        service = new UserCrudServiceImpl(userRepository);
    }

    @Nested
    class FindBaseUserById {

        @Test
        void should_find_user_and_adapt_it() throws TechnicalException {
            // Given
            when(userRepository.findById(any())).thenReturn(
                Optional.of(
                    User.builder()
                        .id("user-id")
                        .organizationId("organization-id")
                        .source("source")
                        .sourceId("source-id")
                        .email("jane.doe@gravitee.io")
                        .firstname("Jane")
                        .lastname("Doe")
                        .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                        .build()
                )
            );

            // When
            var user = service.findBaseUserById("userId");

            // Then
            assertThat(user).contains(
                BaseUserEntity.builder()
                    .id("user-id")
                    .organizationId("organization-id")
                    .source("source")
                    .sourceId("source-id")
                    .email("jane.doe@gravitee.io")
                    .firstname("Jane")
                    .lastname("Doe")
                    .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                    .build()
            );
        }

        @Test
        void should_return_empty_when_user_not_found() throws TechnicalException {
            // Given
            var userId = "user-id";
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            // When
            var result = service.findBaseUserById(userId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var userId = "user-id";
            when(userRepository.findById(any())).thenThrow(new TechnicalException("technical exception"));

            // When
            Throwable throwable = catchThrowable(() -> service.findBaseUserById(userId));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class FindBaseUserByIds {

        @Test
        void should_find_users_and_adapt_them() throws TechnicalException {
            // Given
            when(userRepository.findByIds(any(List.class))).thenAnswer(invocation -> {
                List<String> ids = invocation.getArgument(0);
                return ids
                    .stream()
                    .map(id ->
                        User.builder()
                            .id(id)
                            .organizationId("organization-id")
                            .source("source-" + id)
                            .sourceId("source-id-" + id)
                            .email(id + "@gravitee.io")
                            .firstname("Jane " + id)
                            .lastname("Doe " + id)
                            .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                            .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                            .build()
                    )
                    .collect(Collectors.toSet());
            });

            // When
            var user = service.findBaseUsersByIds(List.of("1", "2"));

            // Then
            assertThat(user)
                .hasSize(2)
                .contains(
                    BaseUserEntity.builder()
                        .id("1")
                        .organizationId("organization-id")
                        .source("source-1")
                        .sourceId("source-id-1")
                        .email("1@gravitee.io")
                        .firstname("Jane 1")
                        .lastname("Doe 1")
                        .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                        .build(),
                    BaseUserEntity.builder()
                        .id("2")
                        .organizationId("organization-id")
                        .source("source-2")
                        .sourceId("source-id-2")
                        .email("2@gravitee.io")
                        .firstname("Jane 2")
                        .lastname("Doe 2")
                        .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                        .build()
                );
        }

        @Test
        void should_return_empty_when_user_not_found() throws TechnicalException {
            // Given
            var userId = "user-id";
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            // When
            var result = service.findBaseUserById(userId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var userId = "user-id";
            when(userRepository.findById(any())).thenThrow(new TechnicalException("technical exception"));

            // When
            Throwable throwable = catchThrowable(() -> service.findBaseUserById(userId));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class GetBaseUser {

        @Test
        void should_find_user_and_adapt_id() throws TechnicalException {
            // Given
            when(userRepository.findById(any())).thenReturn(
                Optional.of(
                    User.builder()
                        .id("user-id")
                        .organizationId("organization-id")
                        .source("source")
                        .sourceId("source-id")
                        .email("jane.doe@gravitee.io")
                        .firstname("Jane")
                        .lastname("Doe")
                        .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00Z")))
                        .build()
                )
            );

            // When
            var user = service.getBaseUser("userId");

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getId()).isEqualTo("user-id");
                softly.assertThat(user.getOrganizationId()).isEqualTo("organization-id");
                softly.assertThat(user.getSource()).isEqualTo("source");
                softly.assertThat(user.getSourceId()).isEqualTo("source-id");
                softly.assertThat(user.getEmail()).isEqualTo("jane.doe@gravitee.io");
                softly.assertThat(user.getFirstname()).isEqualTo("Jane");
                softly.assertThat(user.getLastname()).isEqualTo("Doe");
                softly.assertThat(user.getCreatedAt()).isEqualTo("2020-01-01T00:00:00Z");
                softly.assertThat(user.getUpdatedAt()).isEqualTo("2020-01-02T00:00:00Z");
            });
        }

        @Test
        void should_throw_when_user_not_found() throws TechnicalException {
            // Given
            var userId = "user-id";
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.getBaseUser(userId));

            // Then
            assertThat(throwable).isInstanceOf(UserNotFoundException.class).hasMessage("User [" + userId + "] cannot be found.");
        }
    }
}

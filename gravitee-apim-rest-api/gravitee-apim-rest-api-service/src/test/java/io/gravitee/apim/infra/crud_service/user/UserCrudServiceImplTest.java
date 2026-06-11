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

import static fixtures.core.model.BaseUserEntityFixtures.aBaseUserEntity;
import static fixtures.repository.UserFixtures.aRepositoryUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.UserRepositoryInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.EncodedPassword;
import io.gravitee.apim.core.user.model.IdpSource;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class UserCrudServiceImplTest {

    UserRepositoryInMemory userRepository = new UserRepositoryInMemory();
    UserCrudServiceImpl service = new UserCrudServiceImpl(userRepository);

    @BeforeEach
    void setUp() {
        userRepository.reset();
    }

    @Nested
    class FindBaseUserById {

        @Test
        void should_find_user_and_adapt_it() {
            userRepository.initWith(List.of(aRepositoryUser()));

            var user = service.findBaseUserById("user-id");

            assertThat(user).contains(aBaseUserEntity());
        }

        @Test
        void should_return_empty_when_user_not_found() {
            var result = service.findBaseUserById("user-id");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() {
            userRepository.failsWith(new TechnicalException("technical exception"));

            var throwable = catchThrowable(() -> service.findBaseUserById("user-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class FindBaseUserByIds {

        @ParameterizedTest
        @NullAndEmptySource
        void should_return_empty_set_when_user_ids_is_null_or_empty(List<String> userIds) {
            assertThat(service.findBaseUsersByIds(userIds)).isEmpty();
        }

        @Test
        void should_find_users_and_adapt_them() {
            userRepository.initWith(
                List.of(
                    aRepositoryUser("1", "source-1", "source-id-1", "1@gravitee.io", "Jane 1", "Doe 1"),
                    aRepositoryUser("2", "source-2", "source-id-2", "2@gravitee.io", "Jane 2", "Doe 2")
                )
            );

            var users = service.findBaseUsersByIds(List.of("1", "2"));

            assertThat(users)
                .hasSize(2)
                .contains(
                    BaseUserEntity.builder()
                        .id("1")
                        .organizationId("organization-id")
                        .source(IdpSource.of("source-1"))
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
                        .source(IdpSource.of("source-2"))
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
        void should_return_empty_when_no_matching_ids() {
            var result = service.findBaseUsersByIds(List.of("unknown-id"));

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() {
            userRepository.failsWith(new TechnicalException("technical exception"));

            var throwable = catchThrowable(() -> service.findBaseUsersByIds(List.of("user-id")));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class FindBaseUsersByEmail {

        @Test
        void should_find_users_by_email_in_organization_and_adapt_them() {
            userRepository.initWith(
                List.of(
                    aRepositoryUser("1", "source-1", "source-id-1", "same@gravitee.io", "Jane 1", "Doe 1"),
                    aRepositoryUser("2", "source-2", "source-id-2", "same@gravitee.io", "Jane 2", "Doe 2"),
                    User.builder().id("3").organizationId("other-organization").email("same@gravitee.io").build()
                )
            );

            var users = service.findBaseUsersByEmail("organization-id", "same@gravitee.io");

            assertThat(users).extracting(BaseUserEntity::getId).containsExactly("1", "2");
        }

        @Test
        void should_return_empty_when_no_matching_email() {
            userRepository.initWith(List.of(aRepositoryUser()));

            var result = service.findBaseUsersByEmail("organization-id", "unknown@gravitee.io");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() {
            userRepository.failsWith(new TechnicalException("technical exception"));

            var throwable = catchThrowable(() -> service.findBaseUsersByEmail("organization-id", "same@gravitee.io"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class).hasMessageContaining("technical exception");
        }
    }

    @Nested
    class Create {

        @Test
        void should_create_user_and_return_adapted_entity() {
            var result = service.create(aBaseUserEntity("user-id"));

            assertThat(result.getId()).isEqualTo("user-id");
            assertThat(result.getEmail()).isEqualTo("user@example.com");
        }

        @Test
        void should_pass_all_fields_to_repository() {
            var created = Date.from(Instant.parse("2024-01-01T00:00:00Z"));
            var userToCreate = BaseUserEntity.builder()
                .id("user-id")
                .organizationId("org-id")
                .source(IdpSource.GRAVITEE)
                .sourceId("jane@example.com")
                .email("jane@example.com")
                .firstname("Jane")
                .lastname("Doe")
                .createdAt(created)
                .build();

            service.create(userToCreate);

            var sent = userRepository.storage().get("user-id");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(sent.getId()).isEqualTo("user-id");
                soft.assertThat(sent.getOrganizationId()).isEqualTo("org-id");
                soft.assertThat(sent.getSource()).isEqualTo("gravitee");
                soft.assertThat(sent.getSourceId()).isEqualTo("jane@example.com");
                soft.assertThat(sent.getEmail()).isEqualTo("jane@example.com");
                soft.assertThat(sent.getFirstname()).isEqualTo("Jane");
                soft.assertThat(sent.getLastname()).isEqualTo("Doe");
                soft.assertThat(sent.getCreatedAt()).isEqualTo(created);
                soft.assertThat(sent.getPassword()).isNull();
            });
        }

        @Test
        void should_throw_when_repository_fails() {
            userRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() -> service.create(BaseUserEntity.builder().id("user-id").build()));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void should_update_user_and_return_adapted_entity() {
            var userToUpdate = BaseUserEntity.builder()
                .id("user-id")
                .organizationId("org-id")
                .email("updated@example.com")
                .updatedAt(Date.from(Instant.parse("2024-06-01T00:00:00Z")))
                .build();

            var result = service.update(userToUpdate);

            assertThat(result.getId()).isEqualTo("user-id");
            assertThat(result.getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        void should_not_write_password_field() {
            service.update(BaseUserEntity.builder().id("user-id").build());

            assertThat(userRepository.storage().get("user-id").getPassword()).isNull();
        }

        @Test
        void should_throw_when_repository_fails() {
            userRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() -> service.update(BaseUserEntity.builder().id("user-id").build()));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class UpdateAndSetPassword {

        @Test
        void should_write_encoded_password_to_repository() {
            service.updateAndSetPassword(
                BaseUserEntity.builder().id("user-id").email("jane@example.com").build(),
                new EncodedPassword("$2a$hashed")
            );

            assertThat(userRepository.storage().get("user-id").getPassword()).isEqualTo("$2a$hashed");
        }

        @Test
        void should_return_updated_entity() {
            var result = service.updateAndSetPassword(
                BaseUserEntity.builder().id("user-id").email("jane@example.com").build(),
                new EncodedPassword("$2a$hashed")
            );

            assertThat(result.getId()).isEqualTo("user-id");
            assertThat(result.getEmail()).isEqualTo("jane@example.com");
        }

        @Test
        void should_throw_when_repository_fails() {
            userRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() ->
                service.updateAndSetPassword(BaseUserEntity.builder().id("user-id").build(), new EncodedPassword("$2a$hashed"))
            );

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class IsPasswordSet {

        @Test
        void should_return_true_when_password_is_not_blank() {
            userRepository.initWith(List.of(User.builder().id("user-id").password("$2a$hashed").build()));

            assertThat(service.isPasswordSet("user-id")).isTrue();
        }

        @Test
        void should_return_false_when_password_is_blank() {
            userRepository.initWith(List.of(User.builder().id("user-id").password("").build()));

            assertThat(service.isPasswordSet("user-id")).isFalse();
        }

        @Test
        void should_return_false_when_password_is_null() {
            userRepository.initWith(List.of(User.builder().id("user-id").build()));

            assertThat(service.isPasswordSet("user-id")).isFalse();
        }

        @Test
        void should_return_false_when_user_not_found() {
            assertThat(service.isPasswordSet("user-id")).isFalse();
        }

        @Test
        void should_throw_when_repository_fails() {
            userRepository.failsWith(new TechnicalException("error"));

            var throwable = catchThrowable(() -> service.isPasswordSet("user-id"));

            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }
    }

    @Nested
    class GetBaseUser {

        @Test
        void should_find_user_and_adapt_it() {
            userRepository.initWith(List.of(aRepositoryUser()));

            var user = service.getBaseUser("user-id");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getId()).isEqualTo("user-id");
                softly.assertThat(user.getOrganizationId()).isEqualTo("organization-id");
                softly.assertThat(user.getSource()).isEqualTo(IdpSource.of("source"));
                softly.assertThat(user.getSourceId()).isEqualTo("source-id");
                softly.assertThat(user.getEmail()).isEqualTo("jane.doe@gravitee.io");
                softly.assertThat(user.getFirstname()).isEqualTo("Jane");
                softly.assertThat(user.getLastname()).isEqualTo("Doe");
                softly.assertThat(user.getCreatedAt()).isEqualTo("2020-01-01T00:00:00Z");
                softly.assertThat(user.getUpdatedAt()).isEqualTo("2020-01-02T00:00:00Z");
            });
        }

        @Test
        void should_throw_when_user_not_found() {
            var throwable = catchThrowable(() -> service.getBaseUser("user-id"));

            assertThat(throwable).isInstanceOf(UserNotFoundException.class).hasMessage("User [user-id] cannot be found.");
        }
    }
}

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
import static org.mockito.Mockito.when;

import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class EmailRecipientsServiceImplTest {

    private EmailRecipientsService cut;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        cut = new EmailRecipientsServiceImpl(new FreemarkerTemplateProcessor(), userService);
    }

    @Nested
    class ProcessTemplatedMail {

        @Test
        void should_transform_one_email() {
            var templateData = Map.<String, Object>of(
                "api",
                ApiEntity.builder().primaryOwner(PrimaryOwnerEntity.builder().email("po@gravitee.io").build()).build()
            );

            final Set<String> result = cut.processTemplatedRecipients(List.of("${api.primaryOwner.email}"), templateData);
            assertThat(result).hasSize(1).first().isEqualTo("po@gravitee.io");
        }

        @Test
        void should_not_transform_one_email_when_not_templated() {
            final Set<String> result = cut.processTemplatedRecipients(List.of("anEmail@gravitee.io"), Map.of());
            assertThat(result).hasSize(1).first().isEqualTo("anEmail@gravitee.io");
        }

        @Test
        void should_return_empty_collection() {
            final Set<String> result = cut.processTemplatedRecipients(List.of(""), Map.of());
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_collection_when_invalid_template() {
            var templateData = Map.<String, Object>of(
                "api",
                ApiEntity.builder().primaryOwner(PrimaryOwnerEntity.builder().email("po@gravitee.io").build()).build()
            );

            final Set<String> result = cut.processTemplatedRecipients(List.of("${api.primaryOwner......invalid}"), templateData);
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_collection_when_template_lead_to_unknown_property() {
            var templateData = Map.<String, Object>of(
                "api",
                ApiEntity.builder().primaryOwner(PrimaryOwnerEntity.builder().email("").build()).build()
            );

            final Set<String> result = cut.processTemplatedRecipients(List.of("${api.primaryOwner.email}"), templateData);
            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideRecipientsLists")
        void should_be_able_to_parse_multiple_recipients(List<String> recipients, Set<String> expectedOutput) {
            var templateData = Map.<String, Object>of(
                "api",
                ApiEntity.builder().primaryOwner(PrimaryOwnerEntity.builder().email("po@gravitee.io").build()).build()
            );
            final Set<String> result = cut.processTemplatedRecipients(recipients, templateData);
            assertThat(result).isEqualTo(expectedOutput);
        }

        private static Stream<Arguments> provideRecipientsLists() {
            return Stream.of(
                // Separated by spaces
                Arguments.of(
                    List.of("${api.primaryOwner.email} test@test.test mail@mail.gio"),
                    Set.of("po@gravitee.io", "test@test.test", "mail@mail.gio")
                ),
                // Separated by comma
                Arguments.of(
                    List.of("test@test.test,mail@mail.gio,${api.primaryOwner.email}"),
                    Set.of("po@gravitee.io", "test@test.test", "mail@mail.gio")
                ),
                // Separated by semicolon
                Arguments.of(
                    List.of("test@test.test;mail@mail.gio;${api.primaryOwner.email}"),
                    Set.of("po@gravitee.io", "test@test.test", "mail@mail.gio")
                ),
                // Mixing separators and empty values
                Arguments.of(
                    List.of("test@test.test;     mail@mail.gio ;${api.primaryOwner.email},,,,"),
                    Set.of("po@gravitee.io", "test@test.test", "mail@mail.gio")
                )
            );
        }
    }

    @Nested
    class FilteringRegistredUsers {

        private final ExecutionContext executionContext = new ExecutionContext();

        @Test
        void should_return_empty_collection_if_no_user_found_for_a_mail() {
            when(userService.findByEmail(executionContext, "no-user@mail.gio")).thenReturn(List.of());
            final Set<String> result = cut.filterRegisteredUser(executionContext, List.of("no-user@mail.gio"));
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_collection_if_no_user_has_not_opted_in() {
            final UserEntity userEntity = UserEntity.builder().source("gravitee").email("user@mail.gio").build();
            when(userService.findByEmail(executionContext, "user@mail.gio")).thenReturn(List.of(userEntity));
            final Set<String> result = cut.filterRegisteredUser(executionContext, List.of("user@mail.gio"));
            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideOptedInUser")
        void should_return_opted_in_user_email(UserEntity optedInUser) {
            when(userService.findByEmail(executionContext, optedInUser.getEmail())).thenReturn(List.of(optedInUser));
            final Set<String> result = cut.filterRegisteredUser(executionContext, List.of(optedInUser.getEmail()));
            assertThat(result).containsExactly(optedInUser.getEmail());
        }

        private static Stream<Arguments> provideOptedInUser() {
            return Stream.of(
                // A memory user is considered as opted in since we do not set its password in database
                Arguments.of(UserEntity.builder().email("admin@gio.gio").source("memory").build()),
                // A memory user is considered as opted in since if it is ACTIVE and has a password set
                Arguments.of(UserEntity.builder().email("user@gio.gio").status("ACTIVE").password("h4s4p4ssw0rd").build())
            );
        }
    }
}

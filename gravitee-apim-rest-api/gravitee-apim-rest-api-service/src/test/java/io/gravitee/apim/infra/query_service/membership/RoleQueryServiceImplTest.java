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
package io.gravitee.apim.infra.query_service.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.service.common.ReferenceContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoleQueryServiceImplTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");

    RoleRepository roleRepository;

    RoleQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        service = new RoleQueryServiceImpl(roleRepository);
    }

    @Nested
    class FindApiRole {

        @Test
        @SneakyThrows
        void should_query_for_api_role() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.empty());

            // When
            service.findApiRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"));

            // Then
            verify(roleRepository)
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.API,
                    "role-name",
                    "organization-id",
                    RoleReferenceType.ORGANIZATION
                );
        }

        @Test
        @SneakyThrows
        void should_return_requested_role_and_adapt_it() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                    Optional.of(
                        Role
                            .builder()
                            .id("role-id")
                            .name(invocation.getArgument(1))
                            .referenceId(invocation.getArgument(2))
                            .referenceType(invocation.getArgument(3))
                            .scope(invocation.getArgument(0))
                            .createdAt(Date.from(INSTANT_NOW))
                            .updatedAt(Date.from(INSTANT_NOW))
                            .description("role description")
                            .defaultRole(true)
                            .system(true)
                            .permissions(new int[] { 1, 2, 3 })
                            .build()
                    )
                );

            // When
            var result = service.findApiRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"));

            // Then
            Assertions
                .assertThat(result)
                .contains(
                    io.gravitee.apim.core.membership.model.Role
                        .builder()
                        .id("role-id")
                        .name("role-name")
                        .referenceId("organization-id")
                        .referenceType(io.gravitee.apim.core.membership.model.Role.ReferenceType.ORGANIZATION)
                        .scope(io.gravitee.apim.core.membership.model.Role.Scope.API)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .description("role description")
                        .defaultRole(true)
                        .system(true)
                        .permissions(new int[] { 1, 2, 3 })
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findApiRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"))
            );

            // Then
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessage("An error occurs while trying to find api role");
        }
    }

    @Nested
    class FindApplicationRole {

        @Test
        @SneakyThrows
        void should_query_for_application_role() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.empty());

            // When
            service.findApplicationRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"));

            // Then
            verify(roleRepository)
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.APPLICATION,
                    "role-name",
                    "organization-id",
                    RoleReferenceType.ORGANIZATION
                );
        }

        @Test
        @SneakyThrows
        void should_return_requested_role_and_adapt_it() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                    Optional.of(
                        Role
                            .builder()
                            .id("role-id")
                            .name(invocation.getArgument(1))
                            .referenceId(invocation.getArgument(2))
                            .referenceType(invocation.getArgument(3))
                            .scope(invocation.getArgument(0))
                            .createdAt(Date.from(INSTANT_NOW))
                            .updatedAt(Date.from(INSTANT_NOW))
                            .description("role description")
                            .defaultRole(true)
                            .system(true)
                            .permissions(new int[] { 1, 2, 3 })
                            .build()
                    )
                );

            // When
            var result = service.findApplicationRole(
                "role-name",
                new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id")
            );

            // Then
            Assertions
                .assertThat(result)
                .contains(
                    io.gravitee.apim.core.membership.model.Role
                        .builder()
                        .id("role-id")
                        .name("role-name")
                        .referenceId("organization-id")
                        .referenceType(io.gravitee.apim.core.membership.model.Role.ReferenceType.ORGANIZATION)
                        .scope(io.gravitee.apim.core.membership.model.Role.Scope.APPLICATION)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .description("role description")
                        .defaultRole(true)
                        .system(true)
                        .permissions(new int[] { 1, 2, 3 })
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findApplicationRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"))
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find application role");
        }
    }

    @Nested
    class FindIntegrationRole {

        @Test
        @SneakyThrows
        void should_query_for_integration_role() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.empty());

            // When
            service.findIntegrationRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"));

            // Then
            verify(roleRepository)
                .findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.INTEGRATION,
                    "role-name",
                    "organization-id",
                    RoleReferenceType.ORGANIZATION
                );
        }

        @Test
        @SneakyThrows
        void should_return_requested_role_and_adapt_it() {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                    Optional.of(
                        Role
                            .builder()
                            .id("role-id")
                            .name(invocation.getArgument(1))
                            .referenceId(invocation.getArgument(2))
                            .referenceType(invocation.getArgument(3))
                            .scope(invocation.getArgument(0))
                            .createdAt(Date.from(INSTANT_NOW))
                            .updatedAt(Date.from(INSTANT_NOW))
                            .description("role description")
                            .defaultRole(true)
                            .system(true)
                            .permissions(new int[] { 1, 2, 3 })
                            .build()
                    )
                );

            // When
            var result = service.findIntegrationRole(
                "role-name",
                new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id")
            );

            // Then
            Assertions
                .assertThat(result)
                .contains(
                    io.gravitee.apim.core.membership.model.Role
                        .builder()
                        .id("role-id")
                        .name("role-name")
                        .referenceId("organization-id")
                        .referenceType(io.gravitee.apim.core.membership.model.Role.ReferenceType.ORGANIZATION)
                        .scope(io.gravitee.apim.core.membership.model.Role.Scope.INTEGRATION)
                        .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                        .description("role description")
                        .defaultRole(true)
                        .system(true)
                        .permissions(new int[] { 1, 2, 3 })
                        .build()
                );
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any()))
                .thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() ->
                service.findIntegrationRole("role-name", new ReferenceContext(ReferenceContext.Type.ORGANIZATION, "organization-id"))
            );

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find integration role");
        }
    }

    @Nested
    class FindByIds {

        @Test
        @SneakyThrows
        void should_query_all_roles_with_id_in_list() {
            // Given
            when(roleRepository.findAllByIdIn(eq(Set.of("role-id", "other-role"))))
                .thenReturn(Set.of(Role.builder().id("role-id").build()));

            // When
            service.findByIds(Set.of("role-id", "other-role"));

            // Then
            verify(roleRepository, times(1)).findAllByIdIn(Set.of("role-id", "other-role"));
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_if_null_ids() {
            // When
            var result = service.findByIds(null);

            // Then
            verify(roleRepository, never()).findAllByIdIn(any());
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(roleRepository.findAllByIdIn(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByIds(Set.of("role-id", "other-role")));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find role by list of ids");
        }
    }
}

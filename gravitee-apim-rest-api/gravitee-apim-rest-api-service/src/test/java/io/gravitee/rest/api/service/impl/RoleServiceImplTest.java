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

import static io.gravitee.repository.management.model.Audit.AuditProperties.ROLE;
import static io.gravitee.repository.management.model.Role.AuditEvent.ROLE_DELETED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.DefaultRoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.RoleDeletionForbiddenException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @InjectMocks
    RoleServiceImpl sut;

    @Mock
    RoleRepository roleRepository;

    @Mock
    AuditService auditService;

    @Mock
    ConfigService configService;

    @Mock
    MembershipService membershipService;

    @Nested
    class FindAllByOrganization {

        @Test
        @SneakyThrows
        void organisation_not_found() {
            // Given
            when(roleRepository.findAllByReferenceIdAndReferenceType("organization", RoleReferenceType.ORGANIZATION)).thenReturn(Set.of());

            // When
            List<RoleEntity> roles = sut.findAllByOrganization("organization");

            // Then
            assertThat(roles).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_give_the_role() {
            // Given
            when(roleRepository.findAllByReferenceIdAndReferenceType("organization", RoleReferenceType.ORGANIZATION))
                .thenReturn(Set.of(Role.builder().name("Groudon").build()));

            // When
            List<RoleEntity> roles = sut.findAllByOrganization("organization");

            // Then
            assertThat(roles).containsOnly(RoleEntity.builder().name("Groudon").build());
        }
    }

    @Nested
    class Delete {

        @Test
        @SneakyThrows
        void role_not_found() {
            // Given
            ExecutionContext ctx = new ExecutionContext("orgId");
            when(roleRepository.findById(anyString())).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> sut.delete(ctx, "roleId"));

            // Then
            assertThat(throwable).isInstanceOf(RoleNotFoundException.class);
        }

        @ParameterizedTest
        @CsvSource({ "true,true", "true,false", "false,true" })
        @SneakyThrows
        void forbidden_delete_default_or_system_role(boolean system, boolean def) {
            // Given
            ExecutionContext ctx = new ExecutionContext("orgId");
            when(roleRepository.findById(anyString()))
                .thenReturn(
                    Optional.of(Role.builder().name("Groudon").defaultRole(def).system(system).scope(RoleScope.INTEGRATION).build())
                );

            // When
            Throwable throwable = catchThrowable(() -> sut.delete(ctx, "roleId"));

            // Then
            assertThat(throwable).isInstanceOf(RoleDeletionForbiddenException.class);
        }

        @Test
        @SneakyThrows
        void default_scope_not_found() {
            // Given
            RoleScope scope = RoleScope.INTEGRATION;
            ExecutionContext ctx = new ExecutionContext("orgId");
            when(roleRepository.findById(anyString()))
                .thenReturn(Optional.of(Role.builder().name("Groudon").defaultRole(false).system(false).scope(scope).build()));
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION))
                .thenReturn(Set.of());

            // When
            Throwable throwable = catchThrowable(() -> sut.delete(ctx, "roleId"));

            // Then
            assertThat(throwable).isInstanceOf(DefaultRoleNotFoundException.class);
        }

        @Test
        @SneakyThrows
        void success() {
            // Given
            String roleId = "Dimoret";
            RoleScope scope = RoleScope.INTEGRATION;
            ExecutionContext ctx = new ExecutionContext("orgId");
            when(roleRepository.findById(anyString()))
                .thenReturn(Optional.of(Role.builder().name("Groudon").defaultRole(false).system(false).scope(scope).build()));
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION))
                .thenReturn(Set.of(Role.builder().id("default role").defaultRole(true).build()));

            // When
            sut.delete(ctx, roleId);

            // Then

            verify(membershipService).removeRoleUsage(roleId, "default role");

            verify(roleRepository).delete(roleId);

            verify(auditService)
                .createOrganizationAuditLog(
                    any(),
                    eq("orgId"),
                    eq(Map.of(ROLE, scope + ":" + "Groudon")),
                    eq(ROLE_DELETED),
                    any(),
                    any(),
                    any()
                );
        }
    }

    @Nested
    class FindByScope {

        @Test
        @SneakyThrows
        void success() {
            // Given
            RoleScope scope = RoleScope.INTEGRATION;
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION))
                .thenReturn(
                    Set.of(
                        Role.builder().id("Necrozma").name("Necrozma").build(),
                        Role.builder().id("Feurisson").name("Feurisson").build(),
                        Role.builder().id("Kangourex").name("Kangourex").build()
                    )
                );

            // When
            List<RoleEntity> roles = sut.findByScope(io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION, "orgId");

            // Then
            assertThat(roles).hasSize(3);
            assertThat(roles).map(RoleEntity::getName).isSorted();
        }
    }

    @Nested
    class FindByIdAndOrganizationId {

        @Test
        @SneakyThrows
        void success() {
            // Given
            String roleId = "roleId";
            String organizationId = "orgId";
            when(roleRepository.findByIdAndReferenceIdAndReferenceType(roleId, organizationId, RoleReferenceType.ORGANIZATION))
                .thenReturn(Optional.of(Role.builder().name("Hyporoi").build()));

            // When
            Optional<RoleEntity> roles = sut.findByIdAndOrganizationId(roleId, organizationId);

            // Then
            assertThat(roles).map(RoleEntity::getName).contains("Hyporoi");
        }

        @Test
        @SneakyThrows
        void not_found() {
            // Given
            String roleId = "roleId";
            String organizationId = "orgId";
            when(roleRepository.findByIdAndReferenceIdAndReferenceType(roleId, organizationId, RoleReferenceType.ORGANIZATION))
                .thenReturn(Optional.empty());

            // When
            Optional<RoleEntity> roles = sut.findByIdAndOrganizationId(roleId, organizationId);

            // Then
            assertThat(roles).isNotPresent();
        }
    }

    @Nested
    class FindByScopeAndName {

        @Test
        @SneakyThrows
        void success() {
            // Given
            String name = "Arkeapti";
            String organizationId = "orgId";
            var scope = io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION;
            when(
                roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.INTEGRATION,
                    name,
                    organizationId,
                    RoleReferenceType.ORGANIZATION
                )
            )
                .thenReturn(Optional.of(Role.builder().name("Hyporoi").build()));

            // When
            Optional<RoleEntity> roles = sut.findByScopeAndName(scope, name, organizationId);

            // Then
            assertThat(roles).map(RoleEntity::getName).contains("Hyporoi");
        }

        @Test
        @SneakyThrows
        void not_found() {
            // Given
            String name = "Arkeapti";
            String organizationId = "orgId";
            var scope = io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION;
            when(
                roleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                    RoleScope.INTEGRATION,
                    name,
                    organizationId,
                    RoleReferenceType.ORGANIZATION
                )
            )
                .thenReturn(Optional.empty());

            // When
            Optional<RoleEntity> roles = sut.findByScopeAndName(scope, name, organizationId);

            // Then
            assertThat(roles).isNotPresent();
        }
    }
}

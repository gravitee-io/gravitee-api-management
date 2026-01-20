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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DefaultRoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.RoleDeletionForbiddenException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
            when(roleRepository.findAllByReferenceIdAndReferenceType("organization", RoleReferenceType.ORGANIZATION)).thenReturn(
                Set.of(Role.builder().name("Groudon").build())
            );

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
            when(roleRepository.findById(anyString())).thenReturn(
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
            when(roleRepository.findById(anyString())).thenReturn(
                Optional.of(Role.builder().name("Groudon").defaultRole(false).system(false).scope(scope).build())
            );
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION)).thenReturn(
                Set.of()
            );

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
            when(roleRepository.findById(anyString())).thenReturn(
                Optional.of(Role.builder().name("Groudon").defaultRole(false).system(false).scope(scope).build())
            );
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION)).thenReturn(
                Set.of(Role.builder().id("default role").defaultRole(true).build())
            );

            // When
            sut.delete(ctx, roleId);

            // Then

            verify(membershipService).removeRoleUsage(roleId, "default role");

            verify(roleRepository).delete(roleId);

            verify(auditService).createOrganizationAuditLog(
                any(),
                argThat(
                    auditLogData ->
                        auditLogData.getProperties().equals(Map.of(ROLE, scope + ":" + "Groudon")) &&
                        auditLogData.getEvent().equals(ROLE_DELETED)
                )
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
            when(roleRepository.findByScopeAndReferenceIdAndReferenceType(scope, "orgId", RoleReferenceType.ORGANIZATION)).thenReturn(
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
            when(roleRepository.findByIdAndReferenceIdAndReferenceType(roleId, organizationId, RoleReferenceType.ORGANIZATION)).thenReturn(
                Optional.of(Role.builder().name("Hyporoi").build())
            );

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
            when(roleRepository.findByIdAndReferenceIdAndReferenceType(roleId, organizationId, RoleReferenceType.ORGANIZATION)).thenReturn(
                Optional.empty()
            );

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
            ).thenReturn(Optional.of(Role.builder().name("Hyporoi").build()));

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
            ).thenReturn(Optional.empty());

            // When
            Optional<RoleEntity> roles = sut.findByScopeAndName(scope, name, organizationId);

            // Then
            assertThat(roles).isNotPresent();
        }
    }

    @Nested
    class FindByIds {

        @BeforeEach
        void setUp() {
            GraviteeContext.getCurrentRoles().clear();
        }

        @AfterEach
        void tearDown() {
            GraviteeContext.getCurrentRoles().clear();
        }

        @Test
        void should_return_empty_map_when_roleIds_is_null() {
            // When
            Map<String, RoleEntity> result = sut.findByIds(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_map_when_roleIds_is_empty() {
            // When
            Map<String, RoleEntity> result = sut.findByIds(Set.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_roles_from_cache_when_all_present() {
            // Given
            RoleEntity cachedRole1 = RoleEntity.builder().id("role1").name("Role1").build();
            RoleEntity cachedRole2 = RoleEntity.builder().id("role2").name("Role2").build();
            GraviteeContext.getCurrentRoles().put("role1", cachedRole1);
            GraviteeContext.getCurrentRoles().put("role2", cachedRole2);

            // When
            Map<String, RoleEntity> result = sut.findByIds(Set.of("role1", "role2"));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("role1", cachedRole1);
            assertThat(result).containsEntry("role2", cachedRole2);
        }

        @Test
        @SneakyThrows
        void should_fetch_missing_roles_from_repository() {
            // Given
            RoleEntity cachedRole = RoleEntity.builder().id("role1").name("CachedRole").build();
            GraviteeContext.getCurrentRoles().put("role1", cachedRole);

            Role repoRole = Role.builder().id("role2").name("RepoRole").build();
            when(roleRepository.findAllByIdIn(Set.of("role2"))).thenReturn(Set.of(repoRole));

            // When
            Map<String, RoleEntity> result = sut.findByIds(Set.of("role1", "role2"));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("role1").getName()).isEqualTo("CachedRole");
            assertThat(result.get("role2").getName()).isEqualTo("RepoRole");
            // Verify role2 was added to cache
            assertThat(GraviteeContext.getCurrentRoles()).containsKey("role2");
        }

        @Test
        @SneakyThrows
        void should_fetch_all_roles_from_repository_when_cache_empty() {
            // Given
            Role role1 = Role.builder().id("role1").name("Role1").build();
            Role role2 = Role.builder().id("role2").name("Role2").build();
            when(roleRepository.findAllByIdIn(Set.of("role1", "role2"))).thenReturn(Set.of(role1, role2));

            // When
            Map<String, RoleEntity> result = sut.findByIds(Set.of("role1", "role2"));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("role1").getName()).isEqualTo("Role1");
            assertThat(result.get("role2").getName()).isEqualTo("Role2");
        }

        @Test
        @SneakyThrows
        void should_throw_when_repository_fails() {
            // Given
            when(roleRepository.findAllByIdIn(any())).thenThrow(new TechnicalException("DB error"));

            // When
            Throwable throwable = catchThrowable(() -> sut.findByIds(Set.of("role1")));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalManagementException.class);
        }

        @Test
        @SneakyThrows
        void should_return_only_found_roles_when_some_not_exist() {
            // Given
            Role role1 = Role.builder().id("role1").name("Role1").build();
            when(roleRepository.findAllByIdIn(Set.of("role1", "role2"))).thenReturn(Set.of(role1));

            // When
            Map<String, RoleEntity> result = sut.findByIds(Set.of("role1", "role2"));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsKey("role1");
            assertThat(result).doesNotContainKey("role2");
        }
    }
}

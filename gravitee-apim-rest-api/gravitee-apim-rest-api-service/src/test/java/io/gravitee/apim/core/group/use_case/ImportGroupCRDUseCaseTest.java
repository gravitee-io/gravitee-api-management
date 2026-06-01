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
package io.gravitee.apim.core.group.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import inmemory.CRDMembersDomainServiceInMemory;
import inmemory.GroupCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserDomainServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupCRDDomainService;
import io.gravitee.apim.core.group.model.crd.GroupCRDSpec;
import io.gravitee.apim.core.member.domain_service.ValidateCRDMembersDomainService;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.IdpSource;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.infra.domain_service.group.ValidateGroupCRDDomainServiceImpl;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.RoleService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportGroupCRDUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ACTOR_USER_ID = "actor-user-id";
    private static final String GROUP_ID = "abc0a85b-9924-4981-bd71-69295353f5ff";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, ACTOR_USER_ID);

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private final GroupCrudServiceInMemory groupCrudService = new GroupCrudServiceInMemory();

    private final UserDomainServiceInMemory userDomainService = new UserDomainServiceInMemory();

    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private final CRDMembersDomainServiceInMemory membersService = new CRDMembersDomainServiceInMemory();

    private final RoleService roleService = mock(RoleService.class);

    private ImportGroupCRDUseCase cut;

    @BeforeEach
    void setUp() {
        when(
            roleService.findDefaultRoleByScopes(
                ORGANIZATION_ID,
                io.gravitee.rest.api.model.permissions.RoleScope.API,
                io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION,
                io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION
            )
        ).thenReturn(
            List.of(
                RoleEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .scope(io.gravitee.rest.api.model.permissions.RoleScope.API)
                    .defaultRole(true)
                    .name("USER")
                    .build(),
                RoleEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .scope(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION)
                    .defaultRole(true)
                    .name("USER")
                    .build(),
                RoleEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .scope(io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION)
                    .defaultRole(true)
                    .name("USER")
                    .build()
            )
        );

        cut = new ImportGroupCRDUseCase(
            new ValidateGroupCRDDomainServiceImpl(new ValidateCRDMembersDomainService(userDomainService, roleQueryService), roleService),
            groupQueryService,
            groupCrudService,
            membersService
        );
    }

    @Test
    void should_create_group_setting_origin_to_kubernetes() {
        var spec = GroupCRDSpec.builder()
            .id("abc0a85b-9924-4981-bd71-69295353f5ff")
            .name("kubernetes-spec")
            .members(
                new LinkedHashSet<>(
                    Set.of(
                        GroupCRDSpec.Member.builder()
                            .source("memory")
                            .sourceId("api1")
                            .roles(Map.of(RoleScope.API, "OWNER", RoleScope.APPLICATION, "OWNER", RoleScope.INTEGRATION, "OWNER"))
                            .build()
                    )
                )
            );

        cut.execute(new ImportGroupCRDUseCase.Input(AUDIT_INFO, spec.build()));

        var storage = groupCrudService.storage();

        assertSoftly(soft -> {
            soft.assertThat(storage).hasSize(1);
            soft.assertThat(storage.get(GROUP_ID)).isNotNull();
            soft.assertThat(storage.get(GROUP_ID).getOrigin()).isEqualTo(OriginContext.Origin.KUBERNETES.name());
        });
    }

    @Test
    void should_set_default_roles_on_create() {
        when(roleService.findByScopeAndName(io.gravitee.rest.api.model.permissions.RoleScope.API, "OWNER", ORGANIZATION_ID)).thenReturn(
            Optional.of(RoleEntity.builder().name("OWNER").build())
        );
        when(
            roleService.findByScopeAndName(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION, "USER", ORGANIZATION_ID)
        ).thenReturn(Optional.of(RoleEntity.builder().name("USER").build()));
        when(
            roleService.findByScopeAndName(io.gravitee.rest.api.model.permissions.RoleScope.API_PRODUCT, "USER", ORGANIZATION_ID)
        ).thenReturn(Optional.of(RoleEntity.builder().name("USER").build()));

        var spec = GroupCRDSpec.builder()
            .id(GROUP_ID)
            .name("kubernetes-spec")
            .apiRole("OWNER")
            .applicationRole("USER")
            .apiProductRole("USER");

        cut.execute(new ImportGroupCRDUseCase.Input(AUDIT_INFO, spec.build()));

        assertSoftly(soft -> {
            soft.assertThat(membersService.getGroupApiRole(GROUP_ID)).isEqualTo("OWNER");
            soft.assertThat(membersService.getGroupApplicationRole(GROUP_ID)).isEqualTo("USER");
            soft.assertThat(membersService.getGroupApiProductRole(GROUP_ID)).isEqualTo("USER");
        });
    }

    @Test
    void should_reject_import_when_primary_owner_is_added_as_group_member() {
        userDomainService.initWith(
            List.of(
                BaseUserEntity.builder()
                    .organizationId(ORGANIZATION_ID)
                    .id(ACTOR_USER_ID)
                    .source(IdpSource.of("memory"))
                    .sourceId("admin")
                    .build()
            )
        );
        roleQueryService.initWith(
            List.of(
                Role.builder()
                    .name("USER")
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .id(UUID.randomUUID().toString())
                    .scope(Role.Scope.API)
                    .build()
            )
        );

        var spec = GroupCRDSpec.builder()
            .id(GROUP_ID)
            .name("kubernetes-spec")
            .members(
                new LinkedHashSet<>(
                    Set.of(GroupCRDSpec.Member.builder().source("memory").sourceId("admin").roles(Map.of(RoleScope.API, "USER")).build())
                )
            );

        ImportGroupCRDUseCase.Input input = new ImportGroupCRDUseCase.Input(AUDIT_INFO, spec.build());
        assertThatThrownBy(() -> cut.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Unable to import because of errors")
            .hasMessageContaining("can not change the role of primary owner [admin]");
    }

    @Test
    void sanitize_guard_throws_ValidationDomainException_when_value_is_absent() {
        var validationService = mock(ValidateGroupCRDDomainService.class);
        when(validationService.validateAndSanitize(any())).thenReturn(Validator.Result.empty());

        var useCase = new ImportGroupCRDUseCase(
            validationService,
            new GroupQueryServiceInMemory(),
            new GroupCrudServiceInMemory(),
            new CRDMembersDomainServiceInMemory()
        );

        var auditInfo = AuditInfoFixtures.anAuditInfo("org", "env", "user");
        var spec = GroupCRDSpec.builder().id("group-id").name("test").build();

        var input = new ImportGroupCRDUseCase.Input(auditInfo, spec);
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Unable to sanitize CRD spec");
    }
}

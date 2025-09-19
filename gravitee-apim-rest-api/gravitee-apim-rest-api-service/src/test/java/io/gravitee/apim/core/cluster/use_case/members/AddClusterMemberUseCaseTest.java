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
package io.gravitee.apim.core.cluster.use_case.members;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.AbstractUseCaseTest;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.model.AddMember;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddClusterMemberUseCaseTest extends AbstractUseCaseTest {

    private AddClusterMemberUseCase addClusterMemberUseCase;
    private final MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();

    @BeforeEach
    void setUp() {
        addClusterMemberUseCase = new AddClusterMemberUseCase(membershipCrudService, roleQueryServiceInMemory);
        initRoles();
    }

    @Test
    void should_add_cluster_member() {
        AuditInfo audit = AuditInfo.builder().organizationId("org-1").build();
        AddMember addMember = AddMember.builder().roleName("USER").userId("user-1").build();
        // When
        addClusterMemberUseCase.execute(new AddClusterMemberUseCase.Input(audit, addMember, "cluster-1"));
        // Then
        var membership = membershipCrudService.storage().stream().findFirst().orElseThrow();
        assertAll(() ->
            assertThat(membership)
                .usingRecursiveComparison()
                .isEqualTo(
                    Membership.builder()
                        .id(GENERATED_UUID)
                        .memberId("user-1")
                        .memberType(Membership.Type.USER)
                        .referenceType(Membership.ReferenceType.CLUSTER)
                        .referenceId("cluster-1")
                        .roleId("role-1")
                        .source("system")
                        .createdAt(INSTANT_NOW.atZone(TimeProvider.clock().getZone()))
                        .updatedAt(INSTANT_NOW.atZone(TimeProvider.clock().getZone()))
                        .build()
                )
        );
    }

    @Test
    void should_throw_validation_error_when_role_is_primary_owner() {
        AuditInfo audit = AuditInfo.builder().build();
        AddMember addMember = AddMember.builder().roleName("PRIMARY_OWNER").build();

        assertThrows(SinglePrimaryOwnerException.class, () ->
            addClusterMemberUseCase.execute(new AddClusterMemberUseCase.Input(audit, addMember, "cluster-1"))
        );
    }

    private void initRoles() {
        List<Role> roles = List.of(
            Role.builder()
                .id("role-1")
                .scope(Role.Scope.CLUSTER)
                .name("USER")
                .referenceType(Role.ReferenceType.ORGANIZATION)
                .referenceId("org-1")
                .build()
        );
        roleQueryServiceInMemory.initWith(roles);
    }
}

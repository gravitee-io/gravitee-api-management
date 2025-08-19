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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.MembershipDomainServiceInMemory;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.ClusterPermission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClusterPermissionsUseCaseTest {

    private GetClusterPermissionsUseCase getClusterPermissionsUseCase;
    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();

    @BeforeEach
    void setUp() {
        getClusterPermissionsUseCase = new GetClusterPermissionsUseCase(membershipDomainService);
        initMembers();
    }

    @Test
    void should_return_cluster_user_permissions_is_authenticated_not_admin() {
        // When
        var result = getClusterPermissionsUseCase.execute(new GetClusterPermissionsUseCase.Input(true, false, "member-5", "cluster-1"));
        // Then
        assertAll(
            () -> assertThat(result.permissions().size()).isEqualTo(2),
            () -> assertThat(result.permissions().get(ClusterPermission.MEMBER.getName())).isEqualTo(new char[] { READ.getId() }),
            () ->
                assertThat(result.permissions().get(ClusterPermission.DEFINITION.getName()))
                    .isEqualTo(new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() })
        );
    }

    @Test
    void should_return_cluster_user_permissions_is_admin() {
        // When
        var result = getClusterPermissionsUseCase.execute(new GetClusterPermissionsUseCase.Input(true, true, "admin", "cluster-1"));
        // Then
        assertThat(result.permissions().size()).isEqualTo(ClusterPermission.values().length);
        Arrays
            .stream(ClusterPermission.values())
            .forEach(permission ->
                assertThat(result.permissions().get(permission.getName()))
                    .isEqualTo(new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() })
            );
    }

    @Test
    void should_return_cluster_user_permissions_is_not_authenticated() {
        // When
        var result = getClusterPermissionsUseCase.execute(new GetClusterPermissionsUseCase.Input(false, false, "user", "cluster-1"));
        // Then
        assertThat(result.permissions()).isEmpty();
    }

    private void initMembers() {
        List<MemberEntity> members = List.of(
            MemberEntity
                .builder()
                .id("member-5")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .permissions(
                    Map.of(
                        ClusterPermission.MEMBER.getName(),
                        new char[] { READ.getId() },
                        ClusterPermission.DEFINITION.getName(),
                        new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() }
                    )
                )
                .build(),
            MemberEntity
                .builder()
                .id("member-2")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity
                .builder()
                .id("member-3")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-2")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity
                .builder()
                .id("member-4")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-2")
                .type(MembershipMemberType.USER)
                .build(),
            MemberEntity
                .builder()
                .id("member-1")
                .referenceType(MembershipReferenceType.CLUSTER)
                .referenceId("cluster-1")
                .type(MembershipMemberType.USER)
                .build()
        );
        membershipDomainService.initWith(members);
    }
}

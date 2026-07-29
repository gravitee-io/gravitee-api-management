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
package io.gravitee.apim.core.cluster.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import inmemory.ClusterQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.definition.model.cluster.ClusterType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountClustersByLifecycleStateUseCaseTest {

    private static final String ENV_ID = "env-1";

    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private CountClustersByLifecycleStateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CountClustersByLifecycleStateUseCase(clusterQueryService, membershipQueryService);
        clusterQueryService.initWith(
            List.of(
                cluster("c1", ClusterType.KAFKA_CLUSTER, ClusterLifecycleState.DEPLOYED),
                cluster("c2", ClusterType.KAFKA_CLUSTER, ClusterLifecycleState.DEPLOYED),
                cluster("c3", ClusterType.KAFKA_CLUSTER, ClusterLifecycleState.PENDING),
                cluster("c4", ClusterType.KAFKA_CLUSTER, ClusterLifecycleState.UNDEPLOYED),
                cluster("v1", ClusterType.KAFKA_VIRTUAL_CLUSTER, ClusterLifecycleState.DEPLOYED)
            )
        );
    }

    @Test
    void should_count_all_by_state_for_admin() {
        var out = useCase.execute(new CountClustersByLifecycleStateUseCase.Input(ENV_ID, null, true, "admin"));
        assertAll(
            () -> assertThat(out.total()).isEqualTo(5),
            () -> assertThat(out.deployed()).isEqualTo(3),
            () -> assertThat(out.pending()).isEqualTo(1),
            () -> assertThat(out.undeployed()).isEqualTo(1)
        );
    }

    @Test
    void should_scope_counts_by_type() {
        var out = useCase.execute(new CountClustersByLifecycleStateUseCase.Input(ENV_ID, ClusterType.KAFKA_CLUSTER, true, "admin"));
        assertAll(
            () -> assertThat(out.total()).isEqualTo(4),
            () -> assertThat(out.deployed()).isEqualTo(2),
            () -> assertThat(out.pending()).isEqualTo(1),
            () -> assertThat(out.undeployed()).isEqualTo(1)
        );
    }

    @Test
    void should_return_zero_for_non_admin_without_membership() {
        var out = useCase.execute(new CountClustersByLifecycleStateUseCase.Input(ENV_ID, null, false, "nobody"));
        assertThat(out.total()).isZero();
    }

    @Test
    void should_count_only_visible_clusters_for_non_admin() {
        membershipQueryService.initWith(List.of(clusterMembership("member-1", "c1"), clusterMembership("member-1", "c3")));

        var out = useCase.execute(new CountClustersByLifecycleStateUseCase.Input(ENV_ID, null, false, "member-1"));

        assertAll(
            () -> assertThat(out.total()).isEqualTo(2),
            () -> assertThat(out.deployed()).isEqualTo(1), // c1
            () -> assertThat(out.pending()).isEqualTo(1), // c3
            () -> assertThat(out.undeployed()).isZero()
        );
    }

    private static Cluster cluster(String id, ClusterType type, ClusterLifecycleState lifecycleState) {
        return Cluster.builder()
            .id(id)
            .name(id)
            .environmentId(ENV_ID)
            .organizationId("org-1")
            .type(type)
            .lifecycleState(lifecycleState)
            .build();
    }

    private static Membership clusterMembership(String userId, String clusterId) {
        return Membership.builder()
            .referenceId(clusterId)
            .referenceType(Membership.ReferenceType.CLUSTER)
            .memberType(Membership.Type.USER)
            .memberId(userId)
            .build();
    }
}

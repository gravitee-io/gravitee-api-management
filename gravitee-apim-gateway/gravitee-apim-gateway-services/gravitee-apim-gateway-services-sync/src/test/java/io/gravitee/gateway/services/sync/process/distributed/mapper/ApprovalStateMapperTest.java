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
package io.gravitee.gateway.services.sync.process.distributed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.ExecutorLease;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState.Grant;
import io.gravitee.gateway.handlers.api.manager.impl.ApprovalStateJson;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalstate.ApprovalStateDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalStateMapperTest {

    private static final long NOW = 5_000L;
    private static final DeployedApprovalState STATE = new DeployedApprovalState(
        "req-1",
        List.of("alias-1"),
        "env-1",
        "api-1",
        "rule-1",
        "2026-10-01T00:00:00Z",
        "open-1",
        "APPROVED",
        7_000L,
        "a reason",
        new Grant("TIMED", 8_000L, "app-1", "sub-1", "digest-1"),
        new ExecutorLease("node-1", "lease-1", 6_000L),
        9_000L,
        500L,
        800L,
        1_000L,
        7L
    );

    private final ApprovalStateMapper cut = new ApprovalStateMapper(
        new ApprovalStateJson(new ObjectMapper()),
        Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    );

    @Test
    void should_map_a_deployed_approval_state_to_a_distributed_event_and_back() {
        ApprovalStateDeployable deployable = deployed(STATE);

        DistributedEvent event = cut.to(deployable).blockingGet();

        assertThat(event.getId()).isEqualTo("req-1");
        assertThat(event.getType()).isEqualTo(DistributedEventType.APPROVAL_STATE);
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
        assertThat(event.getUpdatedAt()).isEqualTo(new Date(NOW));
        assertThat(cut.to(event).blockingGet()).isEqualTo(deployable);
    }

    @Test
    void should_map_a_state_without_grant_executor_nor_aliases() {
        DeployedApprovalState pending = new DeployedApprovalState(
            "req-1",
            null,
            "env-1",
            "api-1",
            "rule-1",
            "2026-10-01T00:00:00Z",
            "open-1",
            "PENDING",
            7_000L,
            null,
            null,
            null,
            9_000L,
            500L,
            null,
            1_000L,
            0L
        );

        DistributedEvent event = cut.to(deployed(pending)).blockingGet();

        assertThat(cut.to(event).blockingGet().state()).isEqualTo(pending);
    }

    @Test
    void should_carry_the_version_across_nodes() {
        DistributedEvent event = cut.to(deployed(STATE)).blockingGet();

        assertThat(event.getPayload()).contains("\"version\":7");
        assertThat(cut.to(event).blockingGet().state().version()).isEqualTo(7L);
    }

    /** The copy written before must not outlive the state: every node reads the same store. */
    @Test
    void should_distribute_a_state_past_its_keep_until_as_a_removal() {
        DeployedApprovalState expired = new DeployedApprovalState(
            "req-1",
            List.of(),
            "env-1",
            "api-1",
            "rule-1",
            "2026-10-01T00:00:00Z",
            null,
            "EXECUTED",
            null,
            null,
            null,
            null,
            NOW,
            500L,
            null,
            1_000L,
            0L
        );

        DistributedEvent event = cut.to(deployed(expired)).blockingGet();

        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        assertThat(event.getPayload()).isNull();
    }

    @Test
    void should_distribute_an_undeploy_without_payload() {
        ApprovalStateDeployable undeploy = ApprovalStateDeployable.builder()
            .approvalRequestId("req-1")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        DistributedEvent event = cut.to(undeploy).blockingGet();

        assertThat(event.getPayload()).isNull();
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        assertThat(cut.to(event).blockingGet()).isEqualTo(undeploy);
    }

    @Test
    void should_skip_a_distributed_state_without_organization() {
        DistributedEvent event = cut.to(deployed(STATE).organizationId(null)).blockingGet();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut
            .to(DistributedEvent.builder().id("req-1").syncAction(DistributedSyncAction.DEPLOY).payload("wrong").build())
            .test()
            .assertComplete()
            .assertNoValues();
    }

    private static ApprovalStateDeployable deployed(DeployedApprovalState state) {
        return ApprovalStateDeployable.builder()
            .approvalRequestId(state.id())
            .organizationId("org-1")
            .state(state)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }
}

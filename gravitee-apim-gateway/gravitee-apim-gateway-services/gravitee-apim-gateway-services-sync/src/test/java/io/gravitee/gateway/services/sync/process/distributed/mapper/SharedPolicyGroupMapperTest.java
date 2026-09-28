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
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    private SharedPolicyGroupMapper cut;

    @BeforeEach
    void setUp() {
        cut = new SharedPolicyGroupMapper(objectMapper);
    }

    @SneakyThrows
    @Test
    void should_return_distributed_shared_policy_group_event() {
        ReactableSharedPolicyGroup reactableSharedPolicyGroup = new ReactableSharedPolicyGroup();
        reactableSharedPolicyGroup.setId("env-flow-id");
        reactableSharedPolicyGroup.setName("env-flow-name");
        final DistributedEvent distributedEvent = DistributedEvent.builder()
            .id("env-flow-id")
            .payload(objectMapper.writeValueAsString(reactableSharedPolicyGroup))
            .updatedAt(new Date())
            .type(DistributedEventType.SHARED_POLICY_GROUP)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(result -> {
                assertThat(result.sharedPolicyGroupId()).isEqualTo("env-flow-id");
                assertThat(result.reactableSharedPolicyGroup()).satisfies(reactable -> {
                    assertThat(reactable.getId()).isEqualTo("env-flow-id");
                    assertThat(reactable.getName()).isEqualTo("env-flow-name");
                });
                return true;
            })
            .assertComplete();
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut.to(DistributedEvent.builder().payload("wrong").build()).test().assertNoValues().assertComplete();
    }

    @SneakyThrows
    @Test
    void should_keep_environment_id_from_the_distributed_payload() {
        ReactableSharedPolicyGroup reactableSharedPolicyGroup = new ReactableSharedPolicyGroup();
        reactableSharedPolicyGroup.setId("spg-id");
        reactableSharedPolicyGroup.setEnvironmentId("env-b");
        DistributedEvent distributedEvent = DistributedEvent.builder()
            .id("spg-id")
            .payload(objectMapper.writeValueAsString(reactableSharedPolicyGroup))
            .updatedAt(new Date())
            .type(DistributedEventType.SHARED_POLICY_GROUP)
            .syncAction(DistributedSyncAction.DEPLOY)
            .build();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(result -> {
                assertThat(result.environmentId()).isEqualTo("env-b");
                assertThat(result.sharedPolicyGroupId()).isEqualTo("spg-id");
                return true;
            })
            .assertComplete();
    }

    @SneakyThrows
    @Test
    void should_write_environment_id_into_the_undeploy_payload() {
        SharedPolicyGroupReactorDeployable deployable = SharedPolicyGroupReactorDeployable.builder()
            .sharedPolicyGroupId("spg-id")
            .environmentId("env-b")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        cut
            .to(deployable)
            .test()
            .assertValue(event -> {
                assertThat(event.getId()).isEqualTo("spg-id");
                ReactableSharedPolicyGroup payload = objectMapper.readValue(event.getPayload(), ReactableSharedPolicyGroup.class);
                assertThat(payload.getId()).isEqualTo("spg-id");
                assertThat(payload.getEnvironmentId()).isEqualTo("env-b");
                return true;
            })
            .assertComplete();
    }

    @SneakyThrows
    @Test
    void should_round_trip_an_undeploy_and_keep_the_environment() {
        SharedPolicyGroupReactorDeployable deployable = SharedPolicyGroupReactorDeployable.builder()
            .sharedPolicyGroupId("spg-id")
            .environmentId("env-b")
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        DistributedEvent distributedEvent = cut.to(deployable).blockingFirst();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(result -> {
                assertThat(result.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
                assertThat(result.environmentId()).isEqualTo("env-b");
                assertThat(result.sharedPolicyGroupId()).isEqualTo("spg-id");
                assertThat(result.allEnvironments()).isFalse();
                return true;
            })
            .assertComplete();
    }

    @SneakyThrows
    @Test
    void should_round_trip_an_undeploy_of_every_environment() {
        SharedPolicyGroupReactorDeployable deployable = SharedPolicyGroupReactorDeployable.builder()
            .sharedPolicyGroupId("spg-id")
            .allEnvironments(true)
            .syncAction(SyncAction.UNDEPLOY)
            .build();

        DistributedEvent distributedEvent = cut.to(deployable).blockingFirst();

        cut
            .to(distributedEvent)
            .test()
            .assertValue(result -> {
                assertThat(result.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
                assertThat(result.allEnvironments()).isTrue();
                assertThat(result.sharedPolicyGroupId()).isEqualTo("spg-id");
                return true;
            })
            .assertComplete();
    }

    @SneakyThrows
    @Test
    void should_use_the_reactable_id_when_the_deployable_id_is_unset() {
        ReactableSharedPolicyGroup reactableSharedPolicyGroup = new ReactableSharedPolicyGroup();
        reactableSharedPolicyGroup.setId("spg-id");
        reactableSharedPolicyGroup.setEnvironmentId("env-b");
        SharedPolicyGroupReactorDeployable deployable = SharedPolicyGroupReactorDeployable.builder()
            .reactableSharedPolicyGroup(reactableSharedPolicyGroup)
            .syncAction(SyncAction.DEPLOY)
            .build();

        cut
            .to(deployable)
            .test()
            .assertValue(event -> {
                assertThat(event.getId()).isEqualTo("spg-id");
                return true;
            })
            .assertComplete();
    }
}

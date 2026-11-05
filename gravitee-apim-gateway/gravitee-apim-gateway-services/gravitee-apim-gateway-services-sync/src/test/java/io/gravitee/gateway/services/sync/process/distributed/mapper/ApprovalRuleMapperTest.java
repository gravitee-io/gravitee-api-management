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

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule.ApprovalRuleDeployable;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalRuleMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private final ApprovalRuleMapper cut = new ApprovalRuleMapper(objectMapper);
    private final MemoryAppender memoryAppender = new MemoryAppender();

    @BeforeEach
    void captureWarnings() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApprovalRuleMapper.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.WARN);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @AfterEach
    void releaseWarnings() {
        ((Logger) LoggerFactory.getLogger(ApprovalRuleMapper.class)).detachAppender(memoryAppender);
    }

    @Test
    void should_map_a_deployed_approval_rule_to_a_distributed_event_and_back() {
        ApprovalRuleDeployable deployable = deployed();

        DistributedEvent event = cut.to(deployable).blockingGet();

        assertThat(event.getId()).isEqualTo("approval-rule-1");
        assertThat(event.getType()).isEqualTo(DistributedEventType.APPROVAL_RULE);
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
        assertThat(cut.to(event).blockingGet()).isEqualTo(deployable);
    }

    @Test
    void should_distribute_an_undeploy_without_payload() {
        DistributedEvent event = cut
            .to(
                ApprovalRuleDeployable.builder()
                    .approvalRuleId("approval-rule-1")
                    .environmentId("env-1")
                    .syncAction(SyncAction.UNDEPLOY)
                    .build()
            )
            .blockingGet();

        assertThat(event.getPayload()).isNull();
        assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.UNDEPLOY);
        assertThat(cut.to(event).blockingGet()).isEqualTo(
            ApprovalRuleDeployable.builder().approvalRuleId("approval-rule-1").syncAction(SyncAction.UNDEPLOY).build()
        );
    }

    @Test
    void should_have_no_argument_condition_when_the_payload_lists_none() {
        DistributedEvent event = DistributedEvent.builder()
            .id("approval-rule-1")
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload(
                "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
            )
            .build();

        assertThat(cut.to(event).blockingGet().argumentConditions()).isEmpty();
    }

    @Test
    void should_read_a_payload_without_tool_bindings_from_an_older_primary_node() {
        DistributedEvent event = DistributedEvent.builder()
            .id("approval-rule-1")
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload(
                "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
            )
            .build();

        assertThat(cut.to(event).blockingGet().toolBindings()).isEmpty();
    }

    @Test
    void should_read_a_payload_without_tool_entity_ids_from_an_older_primary_node() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        assertThat(cut.to(event).blockingGet().toolEntityIds()).isEmpty();
    }

    @Test
    void should_distribute_a_rule_anchored_only_on_tool_entity_ids() {
        ApprovalRuleDeployable deployable = deployed().toolPattern(null).toolBindings(List.of());

        assertThat(cut.to(cut.to(deployable).blockingGet()).blockingGet()).isEqualTo(deployable);
    }

    @Test
    void should_drop_only_the_blank_tool_entity_ids_and_keep_the_rule() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolEntityIds\": [\"payments.refund\", \" \", null], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        assertThat(cut.to(event).blockingGet().toolEntityIds()).containsExactly("payments.refund");
    }

    @Test
    void should_skip_a_rule_left_with_no_tool_pattern_nor_binding_nor_valid_tool_entity_id() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolEntityIds\": [\" \", null], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_ignore_fields_a_newer_primary_node_adds() {
        DistributedEvent event = DistributedEvent.builder()
            .id("approval-rule-1")
            .syncAction(DistributedSyncAction.DEPLOY)
            .payload(
                "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"futureField\": 1, \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\", \"futureField\": 2}], \"argumentConditions\": [{\"path\": \"amount\", \"operator\": \"GT\", \"value\": \"1\", \"futureField\": 3}], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
            )
            .build();

        ApprovalRuleDeployable deployable = cut.to(event).blockingGet();

        assertThat(deployable.toolBindings()).containsExactly(new ToolBinding("api-1", "transfer"));
        assertThat(deployable.argumentConditions()).containsExactly(new ArgumentCondition("amount", "GT", "1"));
    }

    @Test
    void should_distribute_a_rule_anchored_only_on_tool_bindings() {
        ApprovalRuleDeployable deployable = deployed().toolPattern(null);

        assertThat(cut.to(cut.to(deployable).blockingGet()).blockingGet()).isEqualTo(deployable);
    }

    @Test
    void should_skip_a_deployed_approval_rule_without_organization() {
        DistributedEvent event = cut.to(deployed().organizationId(null)).blockingGet();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_skip_a_deployed_approval_rule_without_tool_pattern_nor_tool_binding_nor_tool_entity_id() {
        DistributedEvent event = cut.to(deployed().toolPattern(" ").toolBindings(List.of()).toolEntityIds(List.of())).blockingGet();

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_drop_only_the_invalid_tool_bindings_and_keep_the_rule() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\"}, {\"apiId\": \" \", \"toolName\": \"secret-tool\"}, null, {\"apiId\": \"api-2\"}], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        ApprovalRuleDeployable deployable = cut.to(event).blockingGet();

        assertThat(deployable).isNotNull();
        assertThat(deployable.toolBindings()).containsExactly(new ToolBinding("api-1", "transfer"));
        assertThat(memoryAppender.getLoggedEvents())
            .singleElement()
            .satisfies(warning -> {
                assertThat(warning.getLevel()).isEqualTo(Level.WARN);
                assertThat(warning.getArgumentArray()).containsExactly("approval-rule-1", 3);
                assertThat(warning.getFormattedMessage()).doesNotContain("secret-tool", "api-2");
            });
    }

    @Test
    void should_keep_a_rule_anchored_on_its_tool_pattern_when_every_tool_binding_is_invalid() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolPattern\": \"payments.*\", \"toolBindings\": [{\"apiId\": \"api-1\"}], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        ApprovalRuleDeployable deployable = cut.to(event).blockingGet();

        assertThat(deployable.toolPattern()).isEqualTo("payments.*");
        assertThat(deployable.toolBindings()).isEmpty();
        assertThat(memoryAppender.getLoggedEvents())
            .singleElement()
            .satisfies(warning -> assertThat(warning.getArgumentArray()).containsExactly("approval-rule-1", 1));
    }

    @Test
    void should_skip_a_rule_left_with_no_tool_pattern_nor_valid_tool_binding() {
        DistributedEvent event = deployEvent(
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"organizationId\": \"org-1\", \"toolBindings\": [null, {\"apiId\": \"api-1\", \"toolName\": \" \"}], \"priority\": 10, \"createdAt\": 1000, \"updatedAt\": 2000}"
        );

        cut.to(event).test().assertComplete().assertNoValues();
    }

    @Test
    void should_return_empty_with_wrong_payload() {
        cut
            .to(DistributedEvent.builder().id("approval-rule-1").syncAction(DistributedSyncAction.DEPLOY).payload("wrong").build())
            .test()
            .assertComplete()
            .assertNoValues();
    }

    private static DistributedEvent deployEvent(String payload) {
        return DistributedEvent.builder().id("approval-rule-1").syncAction(DistributedSyncAction.DEPLOY).payload(payload).build();
    }

    private static ApprovalRuleDeployable deployed() {
        return ApprovalRuleDeployable.builder()
            .approvalRuleId("approval-rule-1")
            .environmentId("env-1")
            .organizationId("org-1")
            .toolPattern("payments.*")
            .toolBindings(List.of(new ToolBinding("api-1", "transfer")))
            .toolEntityIds(List.of("payments.refund"))
            .argumentConditions(List.of(new ArgumentCondition("amount", "GT", "1000")))
            .priority(10)
            .createdAt(1000L)
            .updatedAt(2000L)
            .syncAction(SyncAction.DEPLOY)
            .build();
    }
}

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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.approvalrule;

import static org.assertj.core.api.Assertions.assertThat;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ArgumentCondition;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalRule.ToolBinding;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.repository.management.model.Event;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class ApprovalRuleMapperTest {

    private static final String VALID_PAYLOAD =
        "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}";

    private final ApprovalRuleMapper mapper = new ApprovalRuleMapper(new ObjectMapper());

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
    void should_map_a_publish_event_to_a_deployable() {
        Event event = event(
            "evt-1",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"argumentConditions\": [{\"path\": \"amount.value\", \"operator\": \"GT\", \"value\": \"1000\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo("approval-rule-1");
        assertThat(d.environmentId()).isEqualTo("env-1");
        assertThat(d.toolPattern()).isEqualTo("payments.*");
        assertThat(d.argumentConditions()).containsExactly(new ArgumentCondition("amount.value", "GT", "1000"));
        assertThat(d.priority()).isEqualTo(10);
        assertThat(d.createdAt()).isEqualTo(Instant.parse("2026-09-14T00:00:00Z").toEpochMilli());
        assertThat(d.updatedAt()).isEqualTo(Instant.parse("2026-09-15T00:00:00Z").toEpochMilli());
        assertThat(d.syncAction()).isEqualTo(SyncAction.DEPLOY);
    }

    @Test
    void should_map_the_tool_bindings_of_a_publish_event() {
        Event event = event(
            "evt-1",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\"}, {\"apiId\": \"api-2\", \"toolName\": \"send_money\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.toolPattern()).isNull();
        assertThat(d.toolBindings()).containsExactly(new ToolBinding("api-1", "transfer"), new ToolBinding("api-2", "send_money"));
    }

    @Test
    void should_map_the_tool_entity_ids_of_a_publish_event_and_deploy_a_rule_anchored_only_on_them() {
        Event event = event(
            "evt-1",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolEntityIds\": [\"payments.refund\", \"payments.charge\"], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.toolPattern()).isNull();
        assertThat(d.toolBindings()).isEmpty();
        assertThat(d.toolEntityIds()).containsExactly("payments.refund", "payments.charge");
    }

    @Test
    void should_have_no_tool_entity_id_when_an_older_control_plane_sends_none() {
        assertThat(mapper.toDeploy(event("evt-2", VALID_PAYLOAD)).blockingGet().toolEntityIds()).isEmpty();
    }

    @Test
    void should_drop_only_the_blank_tool_entity_ids_and_keep_the_rule() {
        Event event = event(
            "evt-7",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolEntityIds\": [\"payments.refund\", \" \", null, \"payments.refund\"], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.toolEntityIds()).containsExactly("payments.refund");
    }

    @Test
    void should_skip_a_rule_left_with_no_tool_pattern_nor_binding_nor_valid_tool_entity_id() {
        Event event = event(
            "evt-7",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolEntityIds\": [\" \", null], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        assertThat(mapper.toDeploy(event).blockingGet()).isNull();
    }

    @Test
    void should_have_no_tool_binding_when_an_older_control_plane_sends_none() {
        assertThat(mapper.toDeploy(event("evt-2", VALID_PAYLOAD)).blockingGet().toolBindings()).isEmpty();
    }

    @Test
    void should_ignore_fields_a_newer_control_plane_adds() {
        Event event = event(
            "evt-1",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"futureField\": 1, \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\", \"futureField\": 2}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        assertThat(mapper.toDeploy(event).blockingGet().toolBindings()).containsExactly(new ToolBinding("api-1", "transfer"));
    }

    @Test
    void should_have_no_argument_condition_when_the_event_lists_none() {
        assertThat(mapper.toDeploy(event("evt-2", VALID_PAYLOAD)).blockingGet().argumentConditions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \" \", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"yesterday\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolBindings\": [], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolBindings\": [{\"apiId\": \" \", \"toolName\": \"transfer\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolBindings\": [null, {\"apiId\": \"api-1\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}",
        }
    )
    void should_skip_a_publish_event_missing_a_required_field(String payload) {
        assertThat(mapper.toDeploy(event("evt-3", payload)).blockingGet()).isNull();
    }

    @Test
    void should_drop_only_the_invalid_tool_bindings_and_keep_the_rule() {
        Event event = event(
            "evt-7",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\"}, {\"apiId\": \" \", \"toolName\": \"secret-tool\"}, null, {\"apiId\": \"api-2\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.toolBindings()).containsExactly(new ToolBinding("api-1", "transfer"));
        assertThat(memoryAppender.getLoggedEvents())
            .singleElement()
            .satisfies(warning -> {
                assertThat(warning.getLevel()).isEqualTo(Level.WARN);
                assertThat(warning.getArgumentArray()).containsExactly("approval-rule-1", 3);
                assertThat(warning.getFormattedMessage()).doesNotContain("secret-tool", "api-2", "evt-7");
            });
    }

    @Test
    void should_keep_a_rule_anchored_on_its_tool_pattern_when_every_tool_binding_is_invalid() {
        Event event = event(
            "evt-8",
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\", \"toolBindings\": [{\"apiId\": \"api-1\"}], \"priority\": 10, \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\"}"
        );

        ApprovalRuleDeployable d = mapper.toDeploy(event).blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.toolPattern()).isEqualTo("payments.*");
        assertThat(d.toolBindings()).isEmpty();
        assertThat(memoryAppender.getLoggedEvents())
            .extracting(ILoggingEvent::getArgumentArray)
            .containsExactly(new Object[] { "approval-rule-1", 1 });
    }

    @Test
    void should_log_nothing_when_every_tool_binding_is_valid() {
        mapper.toDeploy(event("evt-9", VALID_PAYLOAD)).blockingGet();

        assertThat(memoryAppender.getLoggedEvents()).isEmpty();
    }

    @Test
    void should_skip_a_publish_event_with_an_unreadable_payload() {
        assertThat(mapper.toDeploy(event("evt-4", "not json")).blockingGet()).isNull();
    }

    @Test
    void should_map_an_unpublish_event_with_only_its_identity() {
        ApprovalRuleDeployable d = mapper
            .toUndeploy(event("evt-5", "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\"}"))
            .blockingGet();

        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo("approval-rule-1");
        assertThat(d.environmentId()).isEqualTo("env-1");
        assertThat(d.syncAction()).isEqualTo(SyncAction.UNDEPLOY);
    }

    @Test
    void should_skip_an_unpublish_event_without_an_id() {
        assertThat(mapper.toUndeploy(event("evt-6", "{\"environmentId\": \"env-1\"}")).blockingGet()).isNull();
    }

    private static Event event(String id, String payload) {
        Event event = new Event();
        event.setId(id);
        event.setPayload(payload);
        return event;
    }
}

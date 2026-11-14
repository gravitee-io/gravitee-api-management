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
package io.gravitee.gamma.definition.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalTransportWireTest {

    private final ObjectMapper om = new ObjectMapper();
    private final ApprovalCaller caller = new ApprovalCaller("app-1", "alice");

    @Test
    void a_request_command_round_trips_with_its_arguments() throws Exception {
        var command = ApprovalRequestCommand.builder()
            .approvalRequestId("req-1")
            .kind(ApprovalRequestCommand.Kind.OPEN.name())
            .organizationId("org-1")
            .environmentId("env-1")
            .apiId("api-1")
            .ruleId("rule-1")
            .ruleUpdatedAt("2026-10-31T10:00:00Z")
            .toolName("refund_order")
            .arguments(om.readTree("{\"amount\":120,\"customer\":{\"id\":\"c-1\"}}"))
            .argumentsDigest("abc")
            .caller(caller)
            .deliveryPath("MRTR")
            .gatewayNodeId("node-1")
            .sentAt("2026-10-31T10:00:01Z")
            .build();

        assertThat(om.readValue(om.writeValueAsString(command), ApprovalRequestCommand.class)).isEqualTo(command);
    }

    @Test
    void an_open_command_carries_the_catalog_entity_id_of_the_called_tool() throws Exception {
        var command = ApprovalRequestCommand.builder()
            .approvalRequestId("req-1")
            .kind(ApprovalRequestCommand.Kind.OPEN.name())
            .toolName("open_issue")
            .toolEntityId("github.create_issue")
            .arguments(om.readTree("{\"title\":\"t\"}"))
            .build();

        var read = om.readValue(om.writeValueAsString(command), ApprovalRequestCommand.class);

        assertThat(read.getToolEntityId()).isEqualTo("github.create_issue");
        assertThat(read).isEqualTo(command);
    }

    @Test
    void a_command_from_a_gateway_that_predates_the_entity_id_has_none() throws Exception {
        var json = """
            {"approvalRequestId":"req-1","kind":"OPEN","toolName":"refund_order","fieldAddedLater":{"x":1}}
            """;

        var read = om.readValue(json, ApprovalRequestCommand.class);

        assertThat(read.getToolName()).isEqualTo("refund_order");
        assertThat(read.getToolEntityId()).isNull();
    }

    @Test
    void an_outcome_command_round_trips() throws Exception {
        var command = new ApprovalOutcomeCommand("req-1", "org-1", "env-1", "node-1", "lease-1", "EXECUTED", "2026-10-31T10:00:01Z");

        assertThat(om.readValue(om.writeValueAsString(command), ApprovalOutcomeCommand.class)).isEqualTo(command);
    }

    @Test
    void a_state_round_trips_with_its_grant_and_lease() throws Exception {
        var state = ApprovalState.builder()
            .approvalRequestId("req-1")
            .aliasIds(List.of("req-2"))
            .environmentId("env-1")
            .apiId("api-1")
            .ruleId("rule-1")
            .ruleUpdatedAt("2026-10-31T10:00:00Z")
            .openKey("key")
            .createdAt("2026-10-31T10:00:00Z")
            .startedAt("2026-10-31T10:06:00Z")
            .status("APPROVED")
            .expiresAt("2026-10-31T10:15:00Z")
            .grant(new ApprovalState.Grant("TIMED", "2026-10-31T11:00:00Z", caller, "abc"))
            .executor(new ApprovalState.ExecutorLease("node-1", "lease-1", "2026-10-31T10:06:00Z"))
            .keepUntil("2026-10-31T11:00:00Z")
            .updatedAt("2026-10-31T10:05:00Z")
            .version(7L)
            .build();

        assertThat(om.readValue(om.writeValueAsString(state), ApprovalState.class)).isEqualTo(state);
    }

    @Test
    void a_state_from_a_newer_control_plane_is_still_read() throws Exception {
        var json = """
            {"approvalRequestId":"req-1","environmentId":"env-1","apiId":"api-1","ruleId":"rule-1","ruleUpdatedAt":"t",
             "openKey":"key","createdAt":"t","status":"SOMETHING_NEW","keepUntil":"t","updatedAt":"t","fieldAddedLater":{"x":1}}
            """;

        var state = om.readValue(json, ApprovalState.class);

        assertThat(state.getStatus()).isEqualTo("SOMETHING_NEW");
        assertThat(state.getStartedAt()).isNull();
    }

    @Test
    void leaves_the_arguments_and_the_end_user_out_of_what_is_logged() {
        var endUser = ApprovalCaller.builder().applicationId("app-1").subject("alice@example.com").build();
        var command = ApprovalRequestCommand.builder()
            .approvalRequestId("ar-1")
            .arguments(om.createObjectNode().put("card", "4111111111111111"))
            .build();

        assertThat(endUser.toString()).contains("app-1").doesNotContain("alice@example.com");
        assertThat(command.toString()).contains("ar-1").doesNotContain("4111111111111111");
    }

    @Test
    void leaves_the_approvers_reason_out_of_what_is_logged() {
        var state = ApprovalState.builder().approvalRequestId("ar-1").reason("Refused: customer jane@example.com is under review").build();

        assertThat(state.toString()).contains("ar-1").doesNotContain("jane@example.com");
    }
}

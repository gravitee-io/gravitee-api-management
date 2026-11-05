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
import org.junit.jupiter.api.Test;

class ApprovalRuleWireTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void should_round_trip_through_json() throws Exception {
        ApprovalRule rule = ApprovalRule.builder()
            .id("approval-rule-1")
            .environmentId("env-1")
            .toolPattern("payments.*")
            .argumentConditions(List.of(new ApprovalRule.ArgumentCondition("amount.value", "GT", "1000")))
            .toolBindings(List.of(new ApprovalRule.ToolBinding("api-1", "transfer")))
            .toolEntityIds(List.of("payments.transfer", "payments.refund"))
            .priority(10)
            .createdAt("2026-09-14T00:00:00Z")
            .updatedAt("2026-09-15T00:00:00Z")
            .build();

        ApprovalRule back = om.readValue(om.writeValueAsString(rule), ApprovalRule.class);

        assertThat(back).isEqualTo(rule);
        assertThat(back.getToolEntityIds()).containsExactly("payments.transfer", "payments.refund");
    }

    @Test
    void should_read_a_rule_without_tool_entity_ids_from_an_older_control_plane() throws Exception {
        String json = "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\"}";

        ApprovalRule rule = om.readValue(json, ApprovalRule.class);

        assertThat(rule.getToolEntityIds()).isNull();
    }

    @Test
    void should_keep_an_operator_it_does_not_know() throws Exception {
        String json =
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"argumentConditions\": [{\"path\": \"to\", \"operator\": \"MATCHES_SOMEDAY\", \"value\": \"x\"}]}";

        ApprovalRule rule = om.readValue(json, ApprovalRule.class);

        assertThat(rule.getArgumentConditions()).containsExactly(new ApprovalRule.ArgumentCondition("to", "MATCHES_SOMEDAY", "x"));
    }

    @Test
    void should_read_a_rule_without_tool_bindings_from_an_older_control_plane() throws Exception {
        String json = "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"toolPattern\": \"payments.*\"}";

        ApprovalRule rule = om.readValue(json, ApprovalRule.class);

        assertThat(rule.getToolPattern()).isEqualTo("payments.*");
        assertThat(rule.getToolBindings()).isNull();
    }

    @Test
    void should_ignore_fields_a_newer_control_plane_adds() throws Exception {
        String json =
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"futureField\": {\"a\": 1}," +
            " \"argumentConditions\": [{\"path\": \"to\", \"operator\": \"EQ\", \"value\": \"x\", \"futureField\": true}]," +
            " \"toolBindings\": [{\"apiId\": \"api-1\", \"toolName\": \"transfer\", \"futureField\": \"y\"}]}";

        ApprovalRule rule = om.readValue(json, ApprovalRule.class);

        assertThat(rule.getArgumentConditions()).containsExactly(new ApprovalRule.ArgumentCondition("to", "EQ", "x"));
        assertThat(rule.getToolBindings()).containsExactly(new ApprovalRule.ToolBinding("api-1", "transfer"));
    }
}

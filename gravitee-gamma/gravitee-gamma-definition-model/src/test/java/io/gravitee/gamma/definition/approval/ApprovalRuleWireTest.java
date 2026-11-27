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
import java.util.Arrays;
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

    @Test
    void should_be_readable_when_every_field_it_needs_is_valid() {
        assertThat(readableRule().build().unreadableFields()).isEmpty();
    }

    @Test
    void should_name_each_field_that_keeps_it_from_being_evaluated() {
        ApprovalRule rule = readableRule().priority(null).createdAt("yesterday").updatedAt(null).build();

        assertThat(rule.unreadableFields()).containsExactly("priority", "createdAt", "updatedAt");
    }

    @Test
    void should_treat_a_rule_that_names_no_tool_as_readable_when_it_sends_the_empty_lists_aim_sends() {
        ApprovalRule rule = readableRule().toolPattern(null).toolBindings(List.of()).toolEntityIds(List.of()).build();

        assertThat(rule.unreadableFields()).isEmpty();
    }

    @Test
    void should_treat_a_rule_with_only_a_selector_it_does_not_know_as_unreadable() throws Exception {
        String json =
            "{\"id\": \"approval-rule-1\", \"environmentId\": \"env-1\", \"priority\": 10," +
            " \"createdAt\": \"2026-09-14T00:00:00Z\", \"updatedAt\": \"2026-09-15T00:00:00Z\", \"toolTags\": [\"payments\"]}";

        ApprovalRule rule = om.readValue(json, ApprovalRule.class);

        assertThat(rule.unreadableFields()).containsExactly("toolPattern, toolBindings or toolEntityIds");
    }

    @Test
    void should_treat_an_empty_tool_pattern_as_unreadable_not_as_no_pattern() {
        ApprovalRule rule = readableRule().toolPattern("").toolBindings(List.of()).toolEntityIds(List.of()).build();

        assertThat(rule.unreadableFields()).containsExactly("toolPattern, toolBindings or toolEntityIds");
    }

    @Test
    void should_write_only_the_wire_fields_and_none_of_the_derived_views() throws Exception {
        String json = om.writeValueAsString(readableRule().toolBindings(List.of()).toolEntityIds(List.of()).build());

        assertThat(json).doesNotContain("unreadableFields", "usableToolBindings", "usableToolEntityIds");
    }

    @Test
    void should_treat_a_rule_whose_selectors_are_all_unusable_as_unreadable() {
        assertThat(readableRule().toolPattern(" ").build().unreadableFields()).containsExactly(
            "toolPattern, toolBindings or toolEntityIds"
        );
        assertThat(
            readableRule().toolPattern(null).toolBindings(List.of(new ApprovalRule.ToolBinding("", "transfer"))).build().unreadableFields()
        ).containsExactly("toolPattern, toolBindings or toolEntityIds");
    }

    @Test
    void should_expose_only_the_usable_tool_bindings_and_entity_ids() {
        ApprovalRule rule = readableRule()
            .toolBindings(List.of(new ApprovalRule.ToolBinding("api-1", "transfer"), new ApprovalRule.ToolBinding("", "refund")))
            .toolEntityIds(Arrays.asList("payments.refund", "", null))
            .build();

        assertThat(rule.usableToolBindings()).containsExactly(new ApprovalRule.ToolBinding("api-1", "transfer"));
        assertThat(rule.usableToolEntityIds()).containsExactly("payments.refund");
    }

    @Test
    void should_name_an_argument_condition_with_no_path_or_no_operator() {
        assertThat(
            readableRule().argumentConditions(List.of(new ApprovalRule.ArgumentCondition(" ", "GT", "1"))).build().unreadableFields()
        ).containsExactly("argumentConditions");
        assertThat(
            readableRule().argumentConditions(Arrays.asList((ApprovalRule.ArgumentCondition) null)).build().unreadableFields()
        ).containsExactly("argumentConditions");
    }

    private static ApprovalRule.ApprovalRuleBuilder readableRule() {
        return ApprovalRule.builder()
            .id("approval-rule-1")
            .environmentId("env-1")
            .toolPattern("payments.*")
            .priority(10)
            .createdAt("2026-09-14T00:00:00Z")
            .updatedAt("2026-09-15T00:00:00Z");
    }
}

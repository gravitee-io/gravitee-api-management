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
package io.gravitee.repository.elasticsearch.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OtelDataStreamIndexUtilsTest {

    @Test
    void should_replace_hyphens_in_dataset_position_with_underscore() {
        // Dataset-position placeholder: the OTel exporter strips hyphens from the dataset component,
        // so we must do the same on the read side or every query misses.
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel-dev_apim_master", Map.of("orgId", "my-org-1"));
        assertThat(result).isEqualTo("traces-gamma_my_org_1.otel-dev_apim_master");
    }

    @Test
    void should_keep_hyphens_in_namespace_position() {
        // Namespace-position placeholder: the OTel exporter's namespace rule keeps hyphens, so
        // stripping them on the read side would query an index the collector never wrote (this is
        // the bug the position-aware fix addresses for the shipped logs-apim.otel-{orgId} default).
        String result = OtelDataStreamIndexUtils.format("traces-apim.otel-{orgId}", Map.of("orgId", "my-super-org"));
        assertThat(result).isEqualTo("traces-apim.otel-my-super-org");
    }

    @Test
    void should_apply_namespace_rule_to_every_placeholder_right_of_the_otel_marker() {
        // Multi-segment namespace (e.g. "{orgId}-{envId}") — each placeholder lands right of .otel-
        // and keeps its hyphens. The structural hyphen between the two placeholders is part of the
        // template, not a value, so it's preserved untouched.
        var params = new LinkedHashMap<String, String>();
        params.put("orgId", "my-org");
        params.put("envId", "my-env");
        String result = OtelDataStreamIndexUtils.format("logs-apim.otel-{orgId}-{envId}", params);
        assertThat(result).isEqualTo("logs-apim.otel-my-org-my-env");
    }

    @Test
    void should_apply_dataset_rule_when_placeholder_lives_in_both_slots() {
        // Same placeholder used in both dataset and namespace position — pick the stricter rule so
        // the substituted value is legal in the dataset slot too (hyphens are disallowed there).
        // Unusual shape but legal — the namespace ends up with the stripped form too, which is fine.
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel-{orgId}", Map.of("orgId", "my-org"));
        assertThat(result).isEqualTo("traces-gamma_my_org.otel-my_org");
    }

    @Test
    void should_fall_back_to_dataset_rule_when_template_has_no_otel_marker() {
        // Legacy or custom template without the .otel- separator — no position info, so apply the
        // stricter rule. Conservative: never produce an index name the exporter would reject.
        String result = OtelDataStreamIndexUtils.format("custom-{orgId}", Map.of("orgId", "my-org"));
        assertThat(result).isEqualTo("custom-my_org");
    }

    @Test
    void should_lowercase_substituted_values() {
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel", Map.of("orgId", "DEFAULT"));
        assertThat(result).isEqualTo("traces-gamma_default.otel");
    }

    @Test
    void should_replace_every_rune_in_the_dataset_disallowed_set_with_underscore() {
        // Disallowed dataset runes per the OTel ES exporter: -\/*?"<>| ,#:
        // (the hyphen is the dataset-specific addition over the namespace set).
        String result = OtelDataStreamIndexUtils.format("{v}", Map.of("v", "a-b\\c/d*e?f\"g<h>i|j k,l#m:n"));
        assertThat(result).isEqualTo("a_b_c_d_e_f_g_h_i_j_k_l_m_n");
    }

    @Test
    void should_preserve_structural_separators_in_the_template_outside_placeholders() {
        // The substitution is per-VALUE, not per the whole resolved string — so the structural
        // hyphens between traces / gamma / dev_apim_master stay intact.
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel-dev_apim_master", Map.of("orgId", "DEFAULT"));
        assertThat(result).isEqualTo("traces-gamma_default.otel-dev_apim_master");
    }

    @Test
    void should_handle_value_with_no_substitution_needed() {
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel", Map.of("orgId", "validvalue123"));
        assertThat(result).isEqualTo("traces-gamma_validvalue123.otel");
    }

    @Test
    void should_apply_multiple_substitutions_in_one_pass_with_position_aware_rules() {
        // LinkedHashMap to make the ordering explicit — substitution order doesn't matter functionally
        // (no value contains another placeholder pattern), but pinning it makes the assertion stable.
        // Dataset-position placeholders ({module}, {orgId}) get the strict rule; namespace-position
        // ({namespace}) gets the looser rule that preserves hyphens.
        var params = new LinkedHashMap<String, String>();
        params.put("module", "gamma");
        params.put("orgId", "DEFAULT");
        params.put("namespace", "prod-eu");
        String result = OtelDataStreamIndexUtils.format("traces-{module}_{orgId}.otel-{namespace}", params);
        assertThat(result).isEqualTo("traces-gamma_default.otel-prod-eu");
    }

    @Test
    void should_truncate_substituted_value_to_100_bytes() {
        String oversized = "a".repeat(150);
        String result = OtelDataStreamIndexUtils.format("{val}", Map.of("val", oversized));
        assertThat(result).hasSize(100);
    }

    @Test
    void should_leave_template_untouched_when_value_is_empty_string() {
        String result = OtelDataStreamIndexUtils.format("traces-{val}.otel", Map.of("val", ""));
        assertThat(result).isEqualTo("traces-.otel");
    }

    @Test
    void should_not_replace_unrelated_braces_in_the_template() {
        // Placeholder syntax is exactly {name} — a value containing braces isn't re-expanded.
        String result = OtelDataStreamIndexUtils.format("{a}", Map.of("a", "x{b}y"));
        // The substituted value still goes through dataset sanitisation: '{' and '}' are NOT in the
        // disallowed set, so they survive the sanitisation but are lowercased (they're already lower).
        assertThat(result).isEqualTo("x{b}y");
    }
}

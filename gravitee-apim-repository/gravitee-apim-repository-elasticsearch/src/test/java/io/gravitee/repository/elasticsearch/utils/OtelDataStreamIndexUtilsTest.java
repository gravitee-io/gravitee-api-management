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
    void should_replace_hyphens_in_substituted_values_with_underscore() {
        // Original bug: orgId "my-org-1" against a Gravitee-side template stays "my-org-1", but the
        // OTel collector wrote the data stream with hyphens converted to underscores — query misses.
        String result = OtelDataStreamIndexUtils.format("traces-gamma_{orgId}.otel-dev_apim_master", Map.of("orgId", "my-org-1"));
        assertThat(result).isEqualTo("traces-gamma_my_org_1.otel-dev_apim_master");
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
    void should_apply_multiple_substitutions_in_one_pass() {
        // LinkedHashMap to make the ordering explicit — substitution order doesn't matter functionally
        // (no value contains another placeholder pattern), but pinning it makes the assertion stable.
        var params = new LinkedHashMap<String, String>();
        params.put("module", "gamma");
        params.put("orgId", "DEFAULT");
        params.put("namespace", "prod-eu");
        String result = OtelDataStreamIndexUtils.format("traces-{module}_{orgId}.otel-{namespace}", params);
        // namespace placeholder goes through the dataset rule too — hyphens in user-supplied values
        // are universally converted, per the always-strict design choice.
        assertThat(result).isEqualTo("traces-gamma_default.otel-prod_eu");
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

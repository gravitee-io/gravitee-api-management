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
package io.gravitee.definition.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.llm.RequestControls;
import org.junit.jupiter.api.Test;

class RequestControlsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_round_trip_native_controls_and_opt_in_reduction() throws Exception {
        var json = """
            {"maxOutputTokens":4096,"anthropicCache":{"toolsEnd":"1h","systemEnd":"1h","conversationEnd":"5m"},
             "contextReduction":{"enabled":true,"targetInputTokens":60000,"frozenHeadMessages":5,"retainedTailTokens":16000,"cleanAfterMessages":4,"cleanAfterToolSteps":15,"condenseKeepTokens":256,"oversizeMinTokens":4000,"oversizeKeepTokens":1024}}
            """;
        var controls = mapper.readValue(json, RequestControls.class);
        assertThat(controls.maxOutputTokens()).isEqualTo(4096);
        assertThat(controls.anthropicCache().toolsEnd()).isEqualTo(RequestControls.CacheTtl.ONE_HOUR);
        assertThat(controls.contextReduction().targetInputTokens()).isEqualTo(60000);
        assertThat(mapper.readTree(mapper.writeValueAsString(controls))).isEqualTo(mapper.readTree(json));
    }

    @Test
    void should_leave_every_assistance_feature_disabled_when_absent() throws Exception {
        var controls = mapper.readValue("{}", RequestControls.class);
        assertThat(controls.maxOutputTokens()).isNull();
        assertThat(controls.anthropicCache()).isNull();
        assertThat(controls.contextReduction()).isNull();
        assertThat(mapper.writeValueAsString(controls)).isEqualTo("{}");
    }

    @Test
    void should_treat_blank_optional_reduction_groups_as_disabled() throws Exception {
        for (String reduction : new String[] {
            "{}",
            "null",
            "{\"targetInputTokens\":null,\"frozenHeadMessages\":null,\"retainedTailTokens\":null,\"cleanAfterMessages\":null,\"cleanAfterToolSteps\":null,\"condenseKeepTokens\":null,\"oversizeMinTokens\":null,\"oversizeKeepTokens\":null}",
        }) {
            var controls = mapper.readValue("{\"contextReduction\":" + reduction + "}", RequestControls.class);
            assertThat(controls.contextReduction()).isNull();
            assertThat(mapper.writeValueAsString(controls)).isEqualTo("{}");
            assertThat(mapper.readValue(mapper.writeValueAsString(controls), RequestControls.class)).isEqualTo(controls);
        }
        var withCache = mapper.readValue("{\"anthropicCache\":{\"toolsEnd\":\"1h\"},\"contextReduction\":{}}", RequestControls.class);
        assertThat(withCache.contextReduction()).isNull();
        assertThat(withCache.anthropicCache().toolsEnd()).isEqualTo(RequestControls.CacheTtl.ONE_HOUR);
    }

    @Test
    void should_reject_partial_reduction_groups_and_explicit_invalid_targets() {
        for (String field : new String[] {
            "frozenHeadMessages",
            "retainedTailTokens",
            "cleanAfterMessages",
            "cleanAfterToolSteps",
            "condenseKeepTokens",
            "oversizeMinTokens",
            "oversizeKeepTokens",
        }) {
            assertThatThrownBy(() ->
                mapper.readValue("{\"contextReduction\":{\"" + field + "\":0}}", RequestControls.class)
            ).hasRootCauseInstanceOf(IllegalArgumentException.class);
        }
        for (int target : new int[] { 0, -1 }) {
            assertThatThrownBy(() ->
                mapper.readValue("{\"contextReduction\":{\"targetInputTokens\":" + target + "}}", RequestControls.class)
            ).hasRootCauseInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void should_validate_configuration_without_inventing_an_input_limit() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RequestControls(0, null, null));
        assertThatIllegalArgumentException().isThrownBy(() ->
            new RequestControls.ContextReduction(true, 0, null, null, null, null, null, null, null, null, null)
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
            new RequestControls.ContextReduction(true, 100, -1, null, null, null, null, null, null, null, null)
        );
        assertThatThrownBy(() -> mapper.readValue("{\"maxInputTokens\":100}", RequestControls.class));
        assertThatThrownBy(() -> mapper.readValue("{\"anthropicCache\":{\"toolsEnd\":\"forever\"}}", RequestControls.class));
    }

    @Test
    void should_preserve_omitted_overrides_and_zero_disabling_values() throws Exception {
        var minimal = mapper.readValue("{\"enabled\":true,\"targetInputTokens\":1000}", RequestControls.ContextReduction.class);
        assertThat(minimal).isEqualTo(
            new RequestControls.ContextReduction(true, 1000, null, null, null, null, null, null, null, null, null)
        );
        var disabled = new RequestControls.ContextReduction(true, 1000, 0, 0, 0, 0, 0, 0, 0, null, null);
        assertThat(mapper.readValue(mapper.writeValueAsString(disabled), RequestControls.ContextReduction.class)).isEqualTo(disabled);
    }

    @Test
    void should_reject_negative_retention_controls() {
        for (int index = 0; index < 7; index++) {
            Integer[] values = { null, null, null, null, null, null, null };
            values[index] = -1;
            assertThatIllegalArgumentException().isThrownBy(() ->
                new RequestControls.ContextReduction(
                    true,
                    1000,
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    values[4],
                    values[5],
                    values[6],
                    null,
                    null
                )
            );
        }
    }

    @Test
    void should_clear_all_tuning_when_explicitly_off() throws Exception {
        var off = mapper.readValue(
            "{\"enabled\":false,\"targetInputTokens\":123,\"reclaimTargetInputTokens\":12,\"retainedTailMessages\":30,\"condenseKeepTokens\":64}",
            RequestControls.ContextReduction.class
        );
        assertThat(mapper.writeValueAsString(off)).isEqualTo("{\"enabled\":false}");
        assertThatThrownBy(() -> mapper.readValue("{\"enabled\":true}", RequestControls.ContextReduction.class)).hasRootCauseInstanceOf(
            IllegalArgumentException.class
        );
        assertThatThrownBy(() ->
            mapper.readValue("{\"targetInputTokens\":123}", RequestControls.ContextReduction.class)
        ).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_validate_reclaim_watermarks_and_message_tail() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new RequestControls.ContextReduction(true, 1000, null, null, null, null, null, null, null, 1001, null)
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
            new RequestControls.ContextReduction(true, 1000, null, null, null, null, null, null, null, 0, null)
        );
        assertThatIllegalArgumentException().isThrownBy(() ->
            new RequestControls.ContextReduction(true, 1000, null, null, null, null, null, null, null, 500, -1)
        );
    }
}

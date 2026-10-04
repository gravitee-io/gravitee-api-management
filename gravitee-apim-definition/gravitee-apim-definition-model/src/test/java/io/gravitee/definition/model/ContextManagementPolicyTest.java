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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.llm.ContextManagementPolicy;
import io.gravitee.definition.model.llm.ContextManagementProfiles;
import io.gravitee.definition.model.llm.RequestControls;
import org.junit.jupiter.api.Test;

class ContextManagementPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_serialize_and_deserialize_a_default_policy() throws Exception {
        ContextManagementPolicy policy = new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, 1000, null);

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(policy))).isEqualTo(
            objectMapper.readTree("{\"mode\":\"DEFAULT\",\"profile\":\"CUSTOM\",\"compactThreshold\":1000}")
        );
        assertThat(objectMapper.readValue("{\"mode\":\"DEFAULT\",\"compactThreshold\":1000}", ContextManagementPolicy.class)).isEqualTo(
            policy
        );
    }

    @Test
    void should_require_a_positive_threshold_for_default_and_enforce() {
        assertThat(new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, null, null).getCompactThreshold()).isNull();
        assertThatIllegalArgumentException().isThrownBy(() -> new ContextManagementPolicy(ContextManagementPolicy.Mode.ENFORCE, 0, null));
    }

    @Test
    void should_reject_a_threshold_for_passthrough() {
        assertThatIllegalArgumentException().isThrownBy(() ->
            new ContextManagementPolicy(ContextManagementPolicy.Mode.PASSTHROUGH, 1000, null)
        );
    }

    @Test
    void should_resolve_profiles_from_the_shared_catalog_and_clear_stale_controls() throws Exception {
        var economy = new ContextManagementPolicy(ContextManagementPolicy.Mode.ENFORCE, 7, ContextManagementPolicy.Profile.ECONOMY);
        assertThat(economy.getCompactThreshold()).isEqualTo(240000);
        var stale = new RequestControls(
            128,
            null,
            new RequestControls.ContextReduction(true, 1000, null, null, null, null, null, null, null, null, null)
        );
        var resolved = ContextManagementProfiles.resolve(economy, "ANTHROPIC", stale);
        assertThat(resolved.maxOutputTokens()).isNull();
        assertThat(resolved.contextReduction().enabled()).isFalse();
        assertThat(resolved.contextReduction().targetInputTokens()).isNull();
        assertThat(resolved.anthropicCache()).isEqualTo(
            new RequestControls.AnthropicCache(
                RequestControls.CacheTtl.ONE_HOUR,
                RequestControls.CacheTtl.ONE_HOUR,
                RequestControls.CacheTtl.FIVE_MINUTES
            )
        );
        assertThat(ContextManagementProfiles.resolve(economy, "OPEN_AI", stale).anthropicCache()).isNull();
        var flow = new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, 7, ContextManagementPolicy.Profile.FLOW);
        assertThat(flow.getCompactThreshold()).isNull();
        var reduction = ContextManagementProfiles.resolve(flow, "ANTHROPIC", stale).contextReduction();
        assertThat(reduction.enabled()).isTrue();
        assertThat(reduction.targetInputTokens()).isEqualTo(170000);
        assertThat(reduction.reclaimTargetInputTokens()).isEqualTo(60000);
        assertThat(reduction.retainedTailMessages()).isEqualTo(30);
        assertThat(reduction.frozenHeadMessages()).isEqualTo(10);
        assertThat(reduction.retainedTailTokens()).isZero();
        assertThat(reduction.condenseKeepTokens()).isEqualTo(64);
        assertThat(reduction.oversizeKeepTokens()).isEqualTo(256);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(flow), ContextManagementPolicy.class)).isEqualTo(flow);
        for (var profile : new ContextManagementPolicy.Profile[] {
            ContextManagementPolicy.Profile.ECONOMY,
            ContextManagementPolicy.Profile.FLOW,
        }) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                new ContextManagementPolicy(ContextManagementPolicy.Mode.PASSTHROUGH, null, profile)
            );
        }
    }

    @Test
    void should_leave_an_untouched_optional_policy_inherited() throws Exception {
        for (String json : new String[] {
            "{}",
            "{\"profile\":\"CUSTOM\"}",
            "{\"mode\":null,\"profile\":\"CUSTOM\",\"compactThreshold\":null}",
        }) {
            assertThat(objectMapper.readValue(json, ContextManagementPolicy.class)).isNull();
        }
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            objectMapper.readValue("{\"profile\":\"ECONOMY\"}", ContextManagementPolicy.class)
        ).hasRootCauseInstanceOf(NullPointerException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            objectMapper.readValue("{\"compactThreshold\":240000}", ContextManagementPolicy.class)
        ).hasRootCauseInstanceOf(NullPointerException.class);
    }
}

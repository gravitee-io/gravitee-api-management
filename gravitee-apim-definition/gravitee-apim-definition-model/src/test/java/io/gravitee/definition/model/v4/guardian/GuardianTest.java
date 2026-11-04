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
package io.gravitee.definition.model.v4.guardian;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianTest {

    @Test
    void should_return_no_plugin_when_policies_are_null() {
        assertThat(Guardian.builder().policies(null).build().getPlugins()).isEmpty();
    }

    @Test
    void should_return_plugins_of_enabled_policies_only() {
        Guardian guardian = Guardian.builder()
            .policies(
                List.of(
                    Step.builder().policy("policy-enabled").enabled(true).build(),
                    Step.builder().policy("policy-disabled").enabled(false).build()
                )
            )
            .build();

        assertThat(guardian.getPlugins()).containsExactly(new Plugin("policy", "policy-enabled"));
    }

    @Test
    void should_return_plugins_of_enabled_resources_and_policies() {
        Guardian guardian = Guardian.builder()
            .resources(
                List.of(
                    Resource.builder().name("cache").type("cache").enabled(true).build(),
                    Resource.builder().name("disabled").type("oauth2").enabled(false).build()
                )
            )
            .policies(List.of(Step.builder().policy("policy-enabled").enabled(true).build()))
            .build();

        assertThat(guardian.getPlugins()).containsExactly(new Plugin("resource", "cache"), new Plugin("policy", "policy-enabled"));
    }

    @Test
    void should_be_compatible_with_any_api_type_when_no_api_type_is_declared() {
        assertThat(Guardian.builder().build().isCompatibleWith(ApiType.LLM_PROXY)).isTrue();
        assertThat(Guardian.builder().apiTypes(Set.of()).build().isCompatibleWith(ApiType.MCP_PROXY)).isTrue();
        assertThat(Guardian.builder().build().isCompatibleWith(null)).isTrue();
    }

    @Test
    void should_be_compatible_only_with_the_declared_api_types() {
        Guardian guardian = Guardian.builder().apiTypes(Set.of(ApiType.LLM_PROXY, ApiType.A2A_PROXY)).build();

        assertThat(guardian.isCompatibleWith(ApiType.LLM_PROXY)).isTrue();
        assertThat(guardian.isCompatibleWith(ApiType.A2A_PROXY)).isTrue();
        assertThat(guardian.isCompatibleWith(ApiType.MCP_PROXY)).isFalse();
        assertThat(guardian.isCompatibleWith(ApiType.PROXY)).isFalse();
        assertThat(guardian.isCompatibleWith(null)).isFalse();
    }
}

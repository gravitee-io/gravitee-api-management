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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiTest {

    @Test
    void should_include_entrypoint_connect_policies_in_dependencies() {
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .id("test-api")
                .flows(
                    List.of(
                        NativeFlow.builder()
                            .enabled(true)
                            .entrypointConnect(List.of(Step.builder().enabled(true).policy("ip-filtering").configuration("{}").build()))
                            .build()
                    )
                )
                .build()
        );

        Set<Policy> dependencies = nativeApi.dependencies(Policy.class);

        assertThat(dependencies).hasSize(1).extracting(Policy::getName).containsExactly("ip-filtering");
    }

    @Test
    void should_include_all_flow_phase_policies_in_dependencies() {
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .id("test-api")
                .flows(
                    List.of(
                        NativeFlow.builder()
                            .enabled(true)
                            .entrypointConnect(List.of(Step.builder().enabled(true).policy("ip-filtering").configuration("{}").build()))
                            .interact(List.of(Step.builder().enabled(true).policy("transform-headers").configuration("{}").build()))
                            .publish(List.of(Step.builder().enabled(true).policy("json-validation").configuration("{}").build()))
                            .subscribe(List.of(Step.builder().enabled(true).policy("message-filtering").configuration("{}").build()))
                            .build()
                    )
                )
                .build()
        );

        Set<Policy> dependencies = nativeApi.dependencies(Policy.class);

        assertThat(dependencies)
            .hasSize(4)
            .extracting(Policy::getName)
            .containsExactlyInAnyOrder("ip-filtering", "transform-headers", "json-validation", "message-filtering");
    }

    @Test
    void should_not_include_disabled_entrypoint_connect_policies_in_dependencies() {
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .id("test-api")
                .flows(
                    List.of(
                        NativeFlow.builder()
                            .enabled(true)
                            .entrypointConnect(List.of(Step.builder().enabled(false).policy("ip-filtering").configuration("{}").build()))
                            .build()
                    )
                )
                .build()
        );

        Set<Policy> dependencies = nativeApi.dependencies(Policy.class);

        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_not_include_policies_from_disabled_flows() {
        final NativeApi nativeApi = new NativeApi(
            io.gravitee.definition.model.v4.nativeapi.NativeApi.builder()
                .id("test-api")
                .flows(
                    List.of(
                        NativeFlow.builder()
                            .enabled(false)
                            .entrypointConnect(List.of(Step.builder().enabled(true).policy("ip-filtering").configuration("{}").build()))
                            .build()
                    )
                )
                .build()
        );

        Set<Policy> dependencies = nativeApi.dependencies(Policy.class);

        assertThat(dependencies).isEmpty();
    }
}

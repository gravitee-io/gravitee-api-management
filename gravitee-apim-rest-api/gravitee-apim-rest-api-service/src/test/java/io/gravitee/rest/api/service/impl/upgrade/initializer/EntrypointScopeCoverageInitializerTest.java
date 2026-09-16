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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.EntrypointPluginQueryServiceInMemory;
import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EntrypointScopeCoverageInitializerTest {

    private final EntrypointPluginQueryServiceInMemory entrypointPlugins = new EntrypointPluginQueryServiceInMemory();
    private final EntrypointScopeCoverageInitializer initializer = new EntrypointScopeCoverageInitializer(entrypointPlugins);

    @AfterEach
    void tearDown() {
        entrypointPlugins.reset();
    }

    @Test
    void should_report_installed_entrypoints_the_registry_does_not_declare_deployed_or_not() {
        entrypointPlugins.initWith(
            List.of(entrypoint("http-proxy", true), entrypoint("acme-custom", true), entrypoint("acme-beta", false))
        );

        assertThat(initializer.undecidedEntrypointIds()).containsExactly("acme-beta", "acme-custom");
    }

    @Test
    void should_treat_excluded_and_dedicated_family_entrypoints_as_decided() {
        entrypointPlugins.initWith(List.of(entrypoint("sse", true), entrypoint("authzen", true), entrypoint("native-kafka", true)));

        assertThat(initializer.undecidedEntrypointIds()).isEmpty();
    }

    @Test
    void should_never_block_startup() {
        entrypointPlugins.initWith(List.of(entrypoint("acme-custom", true)));

        assertThat(initializer.initialize()).isTrue();
    }

    private static ConnectorPlugin entrypoint(String id, boolean deployed) {
        return ConnectorPlugin.builder()
            .id(id)
            .name(id)
            .version("1.0.0")
            .supportedApiType(ApiType.PROXY)
            .supportedListenerType(ListenerType.HTTP)
            .deployed(deployed)
            .build();
    }
}

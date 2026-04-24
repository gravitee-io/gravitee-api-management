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
package io.gravitee.repository.hazelcast;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.hazelcast.spring.PluginConfiguration;
import org.junit.jupiter.api.Test;

class HazelcastRepositoryProviderTest {

    private final HazelcastRepositoryProvider provider = new HazelcastRepositoryProvider();

    @Test
    void should_return_hazelcast_type() {
        assertThat(provider.type()).isEqualTo("hazelcast");
    }

    @Test
    void should_scope_to_rate_limit_only() {
        assertThat(provider.scopes()).containsExactly(Scope.RATE_LIMIT);
    }

    @Test
    void should_return_plugin_configuration_class_for_rate_limit_scope() {
        assertThat(provider.configuration(Scope.RATE_LIMIT)).isEqualTo(PluginConfiguration.class);
    }

    @Test
    void should_return_null_for_unsupported_scope() {
        assertThat(provider.configuration(Scope.MANAGEMENT)).isNull();
    }
}

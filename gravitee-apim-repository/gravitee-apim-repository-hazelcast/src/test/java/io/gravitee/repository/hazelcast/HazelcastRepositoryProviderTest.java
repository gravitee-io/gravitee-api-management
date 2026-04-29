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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.hazelcast.ratelimit.RateLimitRepositoryConfiguration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HazelcastRepositoryProviderTest {

    private final HazelcastRepositoryProvider provider = new HazelcastRepositoryProvider();

    @Test
    void declares_hazelcast_type() {
        assertThat(provider.type()).isEqualTo("hazelcast");
    }

    @Test
    void declares_only_rate_limit_scope() {
        assertThat(provider.scopes()).containsExactly(Scope.RATE_LIMIT);
    }

    @Test
    void resolves_configuration_for_rate_limit_scope() {
        assertThat(provider.configuration(Scope.RATE_LIMIT)).isEqualTo(RateLimitRepositoryConfiguration.class);
    }

    @Test
    void rejects_unsupported_scopes() {
        assertThatThrownBy(() -> provider.configuration(Scope.MANAGEMENT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MANAGEMENT")
            .hasMessageContaining("RATE_LIMIT");
    }
}

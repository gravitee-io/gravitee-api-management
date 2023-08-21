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
package io.gravitee.repository.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfiguration;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class ElasticsearchRepositoryProviderTest {

    private final ElasticsearchRepositoryProvider provider = new ElasticsearchRepositoryProvider();

    @Test
    public void shouldReturnElasticsearchType() {
        assertThat(provider.type()).isEqualTo("elasticsearch");
    }

    @Test
    public void shouldReturnAnalyticsScope() {
        assertThat(provider.scopes()).containsExactly(Scope.ANALYTICS);
    }

    @Test
    public void shouldReturnElasticSearchConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.ANALYTICS);
        assertThat(configClass).isEqualTo(ElasticsearchRepositoryConfiguration.class);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertThat(configClass).isNull();
    }
}

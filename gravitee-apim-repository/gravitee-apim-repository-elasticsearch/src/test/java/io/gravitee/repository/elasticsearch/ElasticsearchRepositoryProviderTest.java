/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch;

import static org.junit.Assert.*;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.elasticsearch.spring.ElasticsearchRepositoryConfiguration;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ElasticsearchRepositoryProviderTest {

    private final ElasticsearchRepositoryProvider provider = new ElasticsearchRepositoryProvider();

    @Test
    public void shouldReturnElasticsearchType() {
        assertEquals("elasticsearch", provider.type());
    }

    @Test
    public void shouldReturnAnalyticsScope() {
        assertArrayEquals(new Scope[] { Scope.ANALYTICS }, provider.scopes());
    }

    @Test
    public void shouldReturnElasticSearchConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.ANALYTICS);
        assertEquals(ElasticsearchRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertNull(configClass);
    }
}

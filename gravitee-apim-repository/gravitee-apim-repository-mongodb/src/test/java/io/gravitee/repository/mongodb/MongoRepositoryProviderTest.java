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
package io.gravitee.repository.mongodb;

import static org.junit.Assert.*;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.mongodb.management.ManagementRepositoryConfiguration;
import io.gravitee.repository.mongodb.ratelimit.RateLimitRepositoryConfiguration;
import org.junit.Test;

public class MongoRepositoryProviderTest {

    private final MongoRepositoryProvider provider = new MongoRepositoryProvider();

    @Test
    public void shouldReturnJdbcType() {
        assertEquals("mongodb", provider.type());
    }

    @Test
    public void shouldReturnManagementAndRateLimitScope() {
        assertArrayEquals(new Scope[] { Scope.MANAGEMENT, Scope.RATE_LIMIT }, provider.scopes());
    }

    @Test
    public void shouldReturnMongoManagementConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertEquals(ManagementRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnMongoRateLimitConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.RATE_LIMIT);
        assertEquals(RateLimitRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.CACHE);
        assertNull(configClass);
    }
}

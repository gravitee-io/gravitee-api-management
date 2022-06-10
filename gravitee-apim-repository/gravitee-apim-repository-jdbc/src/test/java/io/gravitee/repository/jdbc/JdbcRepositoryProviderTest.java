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
package io.gravitee.repository.jdbc;

import static org.junit.Assert.*;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.jdbc.management.JdbcManagementRepositoryConfiguration;
import io.gravitee.repository.jdbc.ratelimit.JdbcRateLimitRepositoryConfiguration;
import org.junit.Test;

public class JdbcRepositoryProviderTest {

    private final JdbcRepositoryProvider provider = new JdbcRepositoryProvider();

    @Test
    public void shouldReturnJdbcType() {
        assertEquals("jdbc", provider.type());
    }

    @Test
    public void shouldReturnManagementAndRateLimitScope() {
        assertArrayEquals(new Scope[] { Scope.MANAGEMENT, Scope.RATE_LIMIT }, provider.scopes());
    }

    @Test
    public void shouldReturnJdbcManagementConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertEquals(JdbcManagementRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnJdbcRateLimitConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.RATE_LIMIT);
        assertEquals(JdbcRateLimitRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.CACHE);
        assertNull(configClass);
    }
}

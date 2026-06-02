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
package io.gravitee.repository.noop;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.platform.repository.api.Scope;
import org.junit.jupiter.api.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpRepositoryProviderTest {

    private final NoOpRepositoryProvider provider = new NoOpRepositoryProvider();

    @Test
    public void shouldReturnNoOpType() {
        assertEquals("none", provider.type());
    }

    @Test
    public void shouldReturnAnalyticsScope() {
        assertArrayEquals(
            new Scope[] { Scope.ANALYTICS, Scope.MANAGEMENT, Scope.RATE_LIMIT, Scope.OTEL_TRACES, Scope.OTEL_LOGS },
            provider.scopes()
        );
    }

    @Test
    public void shouldReturnNoOpConfigurationClass() {
        assertEquals(NoOpAnalyticsRepositoryConfiguration.class, provider.configuration(Scope.ANALYTICS));
        assertEquals(NoOpManagementRepositoryConfiguration.class, provider.configuration(Scope.MANAGEMENT));
        assertEquals(NoOpRateLimitRepositoryConfiguration.class, provider.configuration(Scope.RATE_LIMIT));
        assertEquals(NoOpOtelTracesRepositoryConfiguration.class, provider.configuration(Scope.OTEL_TRACES));
        assertEquals(NoOpOtelLogsRepositoryConfiguration.class, provider.configuration(Scope.OTEL_LOGS));
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.OAUTH2);
        assertNull(configClass);
    }
}

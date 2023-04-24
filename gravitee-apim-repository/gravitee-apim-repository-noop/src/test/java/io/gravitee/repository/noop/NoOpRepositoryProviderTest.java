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
package io.gravitee.repository.noop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.gravitee.platform.repository.api.Scope;
import org.junit.Test;

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
        assertArrayEquals(new Scope[] { Scope.ANALYTICS }, provider.scopes());
    }

    @Test
    public void shouldReturnNoOpConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.ANALYTICS);
        assertEquals(NoOpRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertNull(configClass);
    }
}

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
package io.gravitee.repository.bridge.client;

import static org.junit.Assert.*;

import io.gravitee.platform.repository.api.Scope;
import io.gravitee.repository.bridge.client.management.ManagementRepositoryConfiguration;
import org.junit.Test;

public class HttpBridgeRepositoryProviderTest {

    private final HttpBridgeRepositoryProvider provider = new HttpBridgeRepositoryProvider();

    @Test
    public void shouldReturnHttpType() {
        assertEquals("http", provider.type());
    }

    @Test
    public void shouldReturnManagementScope() {
        assertArrayEquals(new Scope[] { Scope.MANAGEMENT }, provider.scopes());
    }

    @Test
    public void shouldReturnManagementConfigurationClass() {
        Class<?> configClass = provider.configuration(Scope.MANAGEMENT);
        assertEquals(ManagementRepositoryConfiguration.class, configClass);
    }

    @Test
    public void shouldReturnNullClass() {
        Class<?> configClass = provider.configuration(Scope.CACHE);
        assertNull(configClass);
    }
}

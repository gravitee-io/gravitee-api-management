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
package io.gravitee.plugin.entrypoint.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointPluginManager;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnector;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointFactory;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class EntrypointPluginManagerImplTest {

    private EntrypointPluginManager entrypointPluginManager;

    @BeforeEach
    public void beforeEach() {
        entrypointPluginManager = new EntrypointPluginManagerImpl(new EntrypointClassLoaderFactoryImpl());
    }

    @Test
    public void shouldRegisterNewEntrypointPluginWithoutConfiguration() {
        EntrypointPluginImpl entrypointPlugin = new EntrypointPluginImpl(new FakeEntrypointPlugin(), FakeEntrypointFactory.class, null);
        entrypointPluginManager.register(entrypointPlugin);
        EntrypointConnectorFactory<?> fake = entrypointPluginManager.getFactoryById("fake-entrypoint");
        assertThat(fake).isNotNull();
        EntrypointConnector fakeConnector = fake.createConnector(null);
        assertThat(fakeConnector).isNotNull();
    }

    @Test
    public void shouldRegisterNewEntrypointPluginWithConfiguration() {
        EntrypointPluginImpl entrypointPlugin = new EntrypointPluginImpl(
            new FakeEntrypointPlugin(),
            FakeEntrypointFactory.class,
            EntrypointConnectorConfiguration.class
        );
        entrypointPluginManager.register(entrypointPlugin);
        EntrypointConnectorFactory<?> fake = entrypointPluginManager.getFactoryById("fake-entrypoint");
        assertThat(fake).isNotNull();
        EntrypointConnector fakeConnector = fake.createConnector("{\"info\":\"test\"}");
        assertThat(fakeConnector).isNotNull();
        assertThat(fakeConnector).isInstanceOf(FakeEntrypointConnector.class);
        FakeEntrypointConnector fakeEntrypointConnector = (FakeEntrypointConnector) fakeConnector;
        assertThat(fakeEntrypointConnector.getConfiguration().getInfo()).isEqualTo("test");
    }

    @Test
    public void shouldNotRetrieveUnRegisterPlugin() {
        assertThat(entrypointPluginManager.getFactoryById("fake-entrypoint")).isNull();
    }
}

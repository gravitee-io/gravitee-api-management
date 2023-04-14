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
package io.gravitee.apim.plugin.reactor.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorFactory;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorPlugin;
import io.gravitee.plugin.core.api.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactorPluginHandlerTest {

    @Mock
    private ReactorPluginManager reactorPluginManager;

    private ReactorPluginHandler cut;

    @BeforeEach
    void setUp() {
        cut = new ReactorPluginHandler(reactorPluginManager);
    }

    @Test
    void should_be_reactor_type() {
        assertThat(cut.type()).isEqualTo(ReactorPlugin.PLUGIN_TYPE);
    }

    @Test
    void should_handle_reactor() {
        assertThat(cut.canHandle(new FakeReactorPlugin())).isTrue();
    }

    @Test
    void should_not_handle_reactor() {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.type()).thenReturn("wrong");
        assertThat(cut.canHandle(mockPlugin)).isFalse();
    }

    @Test
    void should_handle_new_plugin() {
        final FakeReactorPlugin fakeReactorPlugin = new FakeReactorPlugin();
        cut.handle(fakeReactorPlugin, FakeReactorFactory.class);
        verify(reactorPluginManager).register(any(ReactorPlugin.class));
    }
}

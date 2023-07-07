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
package io.gravitee.plugin.apiservice.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.apiservice.ApiServicePluginManager;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiServiceFactory;
import io.gravitee.plugin.apiservice.internal.fake.FakeApiServicePlugin;
import io.gravitee.plugin.core.api.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiServicePluginHandlerTest {

    @Mock
    private ApiServicePluginManager apiServicePluginManager;

    private ApiServicePluginHandler cut;

    @BeforeEach
    void init() {
        cut = new ApiServicePluginHandler(apiServicePluginManager);
    }

    @Test
    void should_be_api_service_type() {
        assertThat(cut.type()).isEqualTo(ApiServicePlugin.PLUGIN_TYPE);
    }

    @Test
    void should_handle_api_service() {
        boolean canHandle = cut.canHandle(new FakeApiServicePlugin());
        assertThat(canHandle).isTrue();
    }

    @Test
    void should_not_handle_api_service() {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.type()).thenReturn("bad");
        boolean canHandle = cut.canHandle(mockPlugin);
        assertThat(canHandle).isFalse();
    }

    @Test
    void should_handle_new_plugin() {
        FakeApiServicePlugin plugin = new FakeApiServicePlugin();
        cut.handle(plugin, FakeApiServiceFactory.class);
        verify(apiServicePluginManager).register(any(ApiServicePlugin.class));
    }
}

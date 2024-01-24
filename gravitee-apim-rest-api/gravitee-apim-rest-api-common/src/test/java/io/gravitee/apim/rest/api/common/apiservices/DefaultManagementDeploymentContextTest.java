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
package io.gravitee.apim.rest.api.common.apiservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DefaultManagementDeploymentContextTest {

    @Mock
    private ApplicationContext applicationContext;

    @Test
    void should_get_api() {
        final Api api = new Api();
        final DefaultManagementDeploymentContext cut = new DefaultManagementDeploymentContext(api, applicationContext);
        assertThat(cut.getComponent(Api.class)).isEqualTo(api);
        verify(applicationContext, never()).getBean(any(Class.class));
    }

    @Test
    void should_get_component_from_applicationContext() {
        final PluginConfigurationHelper pluginConfigurationHelper = new PluginConfigurationHelper(null, null);
        when(applicationContext.getBean(PluginConfigurationHelper.class)).thenReturn(pluginConfigurationHelper);
        final DefaultManagementDeploymentContext cut = new DefaultManagementDeploymentContext(new Api(), applicationContext);
        assertThat(cut.getComponent(PluginConfigurationHelper.class)).isEqualTo(pluginConfigurationHelper);
        verify(applicationContext).getBean(any(Class.class));
    }
}

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
package io.gravitee.apim.plugin.reactor.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorFactory;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorPlugin;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.node.plugins.service.ServiceManager;
import io.gravitee.plugin.core.api.PluginContextFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultReactorPluginManagerTest {

    @Mock
    private ConfigurableApplicationContext applicationContext;

    @Mock
    private PluginContextFactory pluginContextFactory;

    @Mock
    private ApplicationContext pluginContext;

    @Mock
    private ReactorFactoryManager reactorFactoryManager;

    @Mock
    private ServiceManager serviceManager;

    private ReactorPluginManager cut;

    @BeforeEach
    void setUp() {
        when(applicationContext.getBean(ReactorFactoryManager.class)).thenReturn(reactorFactoryManager);
        cut = new DefaultReactorPluginManager(applicationContext, pluginContextFactory, serviceManager);
    }

    @Test
    void should_register_new_reactor_plugin() {
        final DefaultReactorPlugin<FakeReactorFactory> reactorPlugin = new DefaultReactorPlugin<>(
            new FakeReactorPlugin(),
            FakeReactorFactory.class
        );
        final FakeReactorFactory fakeReactorFactory = new FakeReactorFactory();
        when(pluginContextFactory.create(reactorPlugin)).thenReturn(pluginContext);
        when(pluginContext.getBean(reactorPlugin.clazz())).thenReturn(fakeReactorFactory);
        final DummyService dummyService = new DummyService();
        when(pluginContext.getBeansOfType(AbstractService.class)).thenReturn(Map.of("dummyService", dummyService));

        when(applicationContext.getBeanFactory()).thenReturn(new DefaultListableBeanFactory());

        cut.register(reactorPlugin);

        verify(reactorFactoryManager).register(fakeReactorFactory);
        verify(serviceManager).register(dummyService);
    }

    private static class DummyService extends AbstractService {}
}

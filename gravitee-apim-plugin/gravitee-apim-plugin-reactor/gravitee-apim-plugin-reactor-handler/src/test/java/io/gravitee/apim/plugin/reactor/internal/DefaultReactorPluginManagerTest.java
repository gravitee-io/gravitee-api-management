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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorFactory;
import io.gravitee.apim.plugin.reactor.internal.fake.FakeReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultReactorPluginManagerTest {

    @Mock
    private ApplicationContext applicationContext;

    private ReactorPluginManager cut;

    @BeforeEach
    void setUp() {
        //        cut = new DefaultReactorPluginManager(applicationContext, pluginContextFactory, new DefaultReactorClassLoaderFactory());
        cut = new DefaultReactorPluginManager(applicationContext, new DefaultReactorClassLoaderFactory());
    }

    @Test
    void should_register_new_engine_plugin() {
        final DefaultReactorPlugin<FakeReactorFactory> enginePlugin = new DefaultReactorPlugin<>(
            new FakeReactorPlugin(),
            FakeReactorFactory.class
        );

        final AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.createBean(FakeReactorFactory.class)).thenReturn(new FakeReactorFactory());

        cut.register(enginePlugin);
        //        final ReactorFactory<?> fakeReactorFactory = cut.getFactoryById("fake-reactor");
        //        assertThat(fakeReactorFactory).isNotNull();
        //
        //        assertThat(cut.getAllFactories()).hasSize(1).contains(fakeReactorFactory);
        //
        //        final ReactorHandler fakeReactor = fakeReactorFactory.create(null);
        //        assertThat(fakeReactor).isNotNull();
    }

    @Test
    void should_not_retrieve_unregistered_plugin() {
        //        final ReactorFactory<?> unregistered = cut.getFactoryById("unregistered");
        //        assertThat(unregistered).isNull();
    }
}

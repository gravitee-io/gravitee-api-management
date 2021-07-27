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
package io.gravitee.gateway.reactor.handler.context;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.reactor.handler.context.provider.NodeTemplateVariableProvider;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class ApiTemplateVariableProviderFactoryTest {

    @Mock
    ApplicationContext applicationContext;

    @InjectMocks
    @Spy
    ApiTemplateVariableProviderFactory templateVariableProviderFactory;

    @Before
    public void setUp() {
        Set<String> names = Set.of(NodeTemplateVariableProvider.class.getName(), TemplateVariableProvider.class.getName());
        doReturn(names).when(templateVariableProviderFactory).loadFactories(isA(ClassLoader.class));
        when(applicationContext.getBean(NodeTemplateVariableProvider.class)).thenReturn(mock(NodeTemplateVariableProvider.class));
        when(applicationContext.getBean(TemplateVariableProvider.class)).thenReturn(mock(TemplateVariableProvider.class));
    }

    @Test
    public void shouldGetTemplateVariableProviderForAPI() {
        List<TemplateVariableProvider> templateVariableProviders = templateVariableProviderFactory.getTemplateVariableProviders();
        assertEquals(templateVariableProviders.size(), 1);
    }
}

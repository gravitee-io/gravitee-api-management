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
package io.gravitee.gateway.debug.handlers.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.endpoint.ref.impl.DefaultReferenceRegister;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContextFactory;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.EnvironmentDictionaryTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.context.ApiTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.gateway.reactor.handler.context.V3ExecutionContextFactory;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class DebugApiContextHandlerFactoryTest {

    @InjectMocks
    private DebugApiReactorHandlerFactory debugApiContextHandlerFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ComponentProvider componentProvider;

    @Mock
    private DefaultReferenceRegister referenceRegister;

    @Mock
    private DictionaryManager dictionaryManager;

    @Test
    public void building_v3ExecutionContextFactory_should_put_ApiTemplateVariableProvider_in_context() {
        ApiTemplateVariableProviderFactory apiTemplateVariableProviderFactory = mock(ApiTemplateVariableProviderFactory.class);
        when(applicationContext.getBeansOfType(TemplateVariableProviderFactory.class))
            .thenReturn(Map.of(ApiTemplateVariableProviderFactory.class.getName(), apiTemplateVariableProviderFactory));
        when(dictionaryManager.createTemplateVariableProvider(any())).thenReturn(mock(EnvironmentDictionaryTemplateVariableProvider.class));

        V3ExecutionContextFactory executionContextFactory = debugApiContextHandlerFactory.v3ExecutionContextFactory(
            mock(Api.class),
            componentProvider,
            referenceRegister
        );

        assertTrue(executionContextFactory instanceof DebugExecutionContextFactory);
        List<TemplateVariableProvider> providers = (List<TemplateVariableProvider>) getField(
            getField(executionContextFactory, "delegate"),
            "providers"
        );
        assertEquals(3, providers.size());
        assertTrue(providers.contains(referenceRegister));
        assertTrue(providers.stream().filter(ApiTemplateVariableProvider.class::isInstance).findAny().isPresent());
        assertTrue(providers.stream().filter(EnvironmentDictionaryTemplateVariableProvider.class::isInstance).findAny().isPresent());
    }
}

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
package io.gravitee.gateway.flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractFlowParametersProviderTest {

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Flow flow;

    @Spy
    private MultiValueMap<String, String> pathParameters;

    @Before
    public void setUp() {
        when(context.request()).thenReturn(request);
        when(request.pathParameters()).thenReturn(pathParameters);
    }

    @Test
    public void shouldNotAddContextRequestPathParameters() {
        when(flow.getPath()).thenReturn("/products");

        abstractFlowParametersProvider.addContextRequestPathParameters(context, flow);

        verify(pathParameters, times(0)).add(anyString(), anyString());
    }

    @Test
    public void shouldAddContextRequestPathParameter() {
        when(flow.getPath()).thenReturn("/products/:productId");
        when(request.pathInfo()).thenReturn("/products/123");

        abstractFlowParametersProvider.addContextRequestPathParameters(context, flow);

        verify(pathParameters, times(1)).add("productId", "123");
    }

    @Test
    public void shouldAddContextRequestPathParameters() {
        when(flow.getPath()).thenReturn("/products/:productId/items/:itemId");
        when(request.pathInfo()).thenReturn("/products/123/items/item-1234");

        abstractFlowParametersProvider.addContextRequestPathParameters(context, flow);

        verify(pathParameters, times(1)).add("productId", "123");
        verify(pathParameters, times(1)).add("itemId", "item-1234");
    }

    private AbstractFlowParametersProvider abstractFlowParametersProvider = new AbstractFlowParametersProvider() {
        @Override
        public StreamableProcessor<ExecutionContext, Buffer> provide(ExecutionContext context) {
            return null;
        }
    };
}

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
package io.gravitee.gateway.handlers.api.processor.pathparameters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.processor.Processor;
import io.gravitee.gateway.handlers.api.path.Path;
import io.gravitee.gateway.handlers.api.path.impl.AbstractPathResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathParametersIndexProcessorTest {

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Handler<ExecutionContext> next;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(context.request()).thenReturn(request);
    }

    @Test
    public void shouldAddPathParamInContext() {
        // Init paths
        PathResolver pathResolver = new PathResolver("/store/:storeId/order/:orderId");

        // Init processor
        Processor<ExecutionContext> processor = new PathParametersIndexProcessor(pathResolver);
        processor.handler(next);

        MultiValueMap<String, String> pathParams = new LinkedMultiValueMap<>();
        when(request.pathInfo()).thenReturn("/store/myStore/order/190783");
        when(request.pathParameters()).thenReturn(pathParams);

        // Run
        processor.handle(context);

        verify(context, times(4)).request(); // one time in getResolvedPath()
        verify(request, times(1)).pathInfo();

        assertEquals(2, pathParams.size());
        assertEquals("myStore", pathParams.getFirst("storeId"));
        assertEquals("190783", pathParams.getFirst("orderId"));
    }

    @Test
    public void shouldAddPathParamInContext_noSubPath() {
        // Init paths
        PathResolver pathResolver = new PathResolver("/:storeId/orders");

        // Init processor
        Processor<ExecutionContext> processor = new PathParametersIndexProcessor(pathResolver);
        processor.handler(next);

        MultiValueMap<String, String> pathParams = new LinkedMultiValueMap<>();
        when(request.pathInfo()).thenReturn("/12345/orders");
        when(request.pathParameters()).thenReturn(pathParams);

        // Run
        processor.handle(context);

        verify(context, times(4)).request(); // one time in getResolvedPath()
        verify(request, times(1)).pathInfo();

        assertEquals(1, pathParams.size());
        assertEquals("12345", pathParams.getFirst("storeId"));
    }

    static class PathResolver extends AbstractPathResolver {

        PathResolver(String sPath) {
            Path path = new Path();
            path.setPath(sPath);
            register(path);
        }
    }
}

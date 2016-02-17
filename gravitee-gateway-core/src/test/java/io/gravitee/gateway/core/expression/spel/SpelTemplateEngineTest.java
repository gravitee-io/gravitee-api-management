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
package io.gravitee.gateway.core.expression.spel;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.impl.ExecutionContextImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class SpelTemplateEngineTest {

    @Mock
    protected Request request;

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void shouldTransformWithRequestHeader() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put("X-Gravitee-Endpoint", "my_api_host");
            }
        });

        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/stores/99/products/123456");

        ExecutionContext executionContext = new ExecutionContextImpl(null);
        executionContext.getTemplateEngine().getTemplateContext().setVariable("request", new WrappedRequestVariable(request));

        String value = executionContext.getTemplateEngine().convert("{#request.headers['X-Gravitee-Endpoint']}");
        Assert.assertEquals("my_api_host", value);
    }

    @Test
    public void shouldTransformWithProperties() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put("X-Gravitee-Endpoint", "my_api_host");
            }
        });

        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/stores/123");

        final Map<String, Object> properties = new HashMap<>();
        properties.put("name_123", "Doe");
        properties.put("firstname_123", "John");

        ExecutionContext executionContext = new ExecutionContextImpl(null);
        executionContext.getTemplateEngine().getTemplateContext().setVariable("request", new WrappedRequestVariable(request));
        executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", properties);

        String value = executionContext.getTemplateEngine().convert("<user><id>{#request.paths[2]}</id><name>{#properties['name_123']}</name><firstname>{#properties['firstname_' + #request.paths[2]]}</firstname></user>");
        Assert.assertNotNull(value);
    }

    @Test
    public void shouldTransformJsonContent() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put("X-Gravitee-Endpoint", "my_api_host");
            }
        });

        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/stores/123");

        final Map<String, Object> properties = new HashMap<>();
        properties.put("name_123", "Doe");
        properties.put("firstname_123", "John");

        ExecutionContext executionContext = new ExecutionContextImpl(null);
        executionContext.getTemplateEngine().getTemplateContext().setVariable("request", new WrappedRequestVariable(request));
        executionContext.getTemplateEngine().getTemplateContext().setVariable("properties", properties);

        String content = "[{id: {#request.paths[2]}, firstname: {#properties['firstname_123']}, name: {#properties['name_123']}, age: 0}]";
        String value = executionContext.getTemplateEngine().convert(content);
        Assert.assertNotNull(value);
    }

    @Test
    public void shouldCallRandomFunction() {
        ExecutionContext executionContext = new ExecutionContextImpl(null);
        String content = "age: {(T(java.lang.Math).random() * 60).intValue()}";
        String value = executionContext.getTemplateEngine().convert(content);
        Assert.assertNotNull(value);
    }
}

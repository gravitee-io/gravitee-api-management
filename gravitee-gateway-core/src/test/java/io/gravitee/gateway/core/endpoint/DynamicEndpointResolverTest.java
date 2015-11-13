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
package io.gravitee.gateway.core.endpoint;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.definition.Api;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DynamicEndpointResolverTest {

    @Mock
    protected Request request;

    @Mock
    protected Api api;

    @Mock
    protected Proxy proxy;

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void test_dynamicEndpoint_usingRequestHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put("X-Gravitee-Endpoint", "my_api_host");
            }
        });

        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/stores/99/products/123456");

        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://{#request.headers['X-Gravitee-Endpoint']}:9099/weather");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://my_api_host:9099/weather", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingRequestParams() {
        final Map<String, String> params = new HashMap<>();
        params.put("env", "prod");

        when(request.parameters()).thenReturn(params);

        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://remote_api:9099/weather/{#request.params['env']}");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://remote_api:9099/weather/prod", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingRequestPaths() {
        when(request.path()).thenReturn("/stores/99/products/123456");
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://store_{#request.paths[2]}_host:9099/weather");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://store_99_host:9099/weather", endpointUri.toString());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void test_dynamicEndpoint_usingRequestPaths_outOfBounds() {
        when(request.path()).thenReturn("/stores");
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://store_{#request.paths[2]}_host:9099/weather");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://store_99_host:9099/weather", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingRequestHeadersAndParams() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put("X-Gravitee-Endpoint", "my_api_host");
            }
        });

        final Map<String, String> params = new HashMap<>();
        params.put("header", "X-Gravitee-Endpoint");

        when(request.parameters()).thenReturn(params);
        when(request.headers()).thenReturn(headers);
        when(request.path()).thenReturn("/stores/99/products/123456");

        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://{#request.headers[#request.params['header']]}:9099/weather");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://my_api_host:9099/weather", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingProperties() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("property_1", "value1");

        when(api.getProxy()).thenReturn(proxy);
        when(api.getProperties()).thenReturn(properties);
        when(proxy.getEndpoint()).thenReturn("http://my_api_host:9099/weather/{#properties['property_1']}");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://my_api_host:9099/weather/value1", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingNullProperties() {
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://my_api_host:9099/weather/{#properties['property_1']}");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://my_api_host:9099/weather/", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingUnknownProperty() {
        final Map<String, Object> properties = new HashMap<>();

        when(api.getProxy()).thenReturn(proxy);
        when(api.getProperties()).thenReturn(properties);
        when(proxy.getEndpoint()).thenReturn("http://my_api_host:9099/weather/{#properties['property_1']}");

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://my_api_host:9099/weather/", endpointUri.toString());
    }

    @Test
    public void test_dynamicEndpoint_usingPropertyAndPath() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("store_99", "host_store_xxx");

        when(request.path()).thenReturn("/stores/99/products/123456");
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getEndpoint()).thenReturn("http://{#properties['store_' + #request.paths[2]]}:9099/weather");
        when(api.getProperties()).thenReturn(properties);

        URI endpointUri = new DynamicEndpointResolver(api).resolve(request);
        Assert.assertNotNull(endpointUri);
        Assert.assertEquals("http://host_store_xxx:9099/weather", endpointUri.toString());
    }
}

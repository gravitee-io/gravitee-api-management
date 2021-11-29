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
package io.gravitee.gateway.core.endpoint.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDefinedProxyEndpointTest {

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromTarget() {
        MultiValueMap<String, String> expectedParameters = new LinkedMultiValueMap<>();
        expectedParameters.add("endpointParam", "v");

        ProxyRequest proxyRequest = new UserDefinedProxyEndpoint(mock(Endpoint.class), "http://host:8080/test?endpointParam=v")
        .createProxyRequest(mock(Request.class));

        Assert.assertNotNull(proxyRequest);
        Assert.assertNotNull(proxyRequest.parameters());

        expectedParameters.forEach((paramKey, paramValue) -> Assert.assertTrue(proxyRequest.parameters().containsKey(paramKey)));
    }

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromRequest() {
        MultiValueMap<String, String> expectedParameters = new LinkedMultiValueMap<>();
        expectedParameters.add("dynroutParam", "v");

        Request request = mock(Request.class);
        MultiValueMap<String, String> requestQueryParameters = new LinkedMultiValueMap<>();
        requestQueryParameters.add("dynroutParam", "v");
        when(request.parameters()).thenReturn(requestQueryParameters);

        ProxyRequest proxyRequest = new UserDefinedProxyEndpoint(mock(Endpoint.class), "http://host:8080/test").createProxyRequest(request);

        Assert.assertNotNull(proxyRequest);
        Assert.assertNotNull(proxyRequest.parameters());

        expectedParameters.forEach((paramKey, paramValue) -> Assert.assertTrue(proxyRequest.parameters().containsKey(paramKey)));
    }

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromRequestAndTarget() {
        MultiValueMap<String, String> expectedParameters = new LinkedMultiValueMap<>();
        expectedParameters.add("dynroutParam", "v");
        expectedParameters.add("endpointParam", "v");

        Request request = mock(Request.class);
        MultiValueMap<String, String> requestQueryParameters = new LinkedMultiValueMap<>();
        requestQueryParameters.add("dynroutParam", "v");
        when(request.parameters()).thenReturn(requestQueryParameters);

        ProxyRequest proxyRequest = new UserDefinedProxyEndpoint(mock(Endpoint.class), "http://host:8080/test?endpointParam=v")
        .createProxyRequest(request);

        Assert.assertNotNull(proxyRequest);
        Assert.assertNotNull(proxyRequest.parameters());

        expectedParameters.forEach((paramKey, paramValue) -> Assert.assertTrue(proxyRequest.parameters().containsKey(paramKey)));
    }
}

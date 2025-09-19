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
package io.gravitee.gateway.core.endpoint.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDefinedProxyEndpointTest {

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromTarget() {
        String queryParamKey = "endpointParam";
        ProxyRequest proxyRequest = new UserDefinedProxyEndpoint(
            mock(Endpoint.class),
            "http://host:8080/test?%s=v".formatted(queryParamKey)
        ).createProxyRequest(mock(Request.class));

        assertThat(proxyRequest.parameters()).containsKeys(queryParamKey);
    }

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromRequest() {
        String queryParamKey = "dynroutParam";
        Request request = mock(Request.class);
        MultiValueMap<String, String> requestQueryParameters = new LinkedMultiValueMap<>();
        requestQueryParameters.add(queryParamKey, "v");
        when(request.parameters()).thenReturn(requestQueryParameters);

        ProxyRequest proxyRequest = new UserDefinedProxyEndpoint(mock(Endpoint.class), "http://host:8080/test").createProxyRequest(request);

        assertThat(proxyRequest.parameters()).containsKeys(queryParamKey);
    }

    @Test
    public void shouldCreateProxyRequest_withQueryParamsFromRequestAndTarget() {
        String queryParamKey = "dynroutParam";
        String urlParamKey = "endpointParam";
        Request request = mock(Request.class);
        MultiValueMap<String, String> requestQueryParameters = new LinkedMultiValueMap<>();
        requestQueryParameters.add(queryParamKey, "v");
        when(request.parameters()).thenReturn(requestQueryParameters);

        var proxyRequest = new UserDefinedProxyEndpoint(
            mock(Endpoint.class),
            "http://host:8080/test?%s=v".formatted(urlParamKey)
        ).createProxyRequest(request);

        assertThat(proxyRequest.parameters()).containsKeys(queryParamKey, urlParamKey);
    }
}

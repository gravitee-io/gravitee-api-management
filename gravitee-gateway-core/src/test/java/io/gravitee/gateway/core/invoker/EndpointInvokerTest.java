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
package io.gravitee.gateway.core.invoker;

import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointInvokerTest {

    @InjectMocks
    EndpointInvoker invoker;

    @Mock
    EndpointResolver endpointResolver;

    @Test
    public void shouldInvoke() {
        compare("http://local.com", "http://local.com", new LinkedMultiValueMap());
    }

    @Test
    public void shouldInvokeWithParam() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("foo", "bar");
        compare("http://local.com?foo=bar", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeWithoutEncodeParam() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("foo", "=bar");
        compare("http://local.com?foo==bar", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeWithoutEncodeEncodedParam() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("foo", "%3Dbar");
        compare("http://local.com?foo=%3Dbar", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeMergedQueryParams() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("urlParams", "value");
        compare("http://local.com?foo=bar&urlParams=value", "http://local.com?foo=bar", parameters);
    }

    @Test
    public void shouldInvokeEmptyQueryParam() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("urlParam", "");
        compare("http://local.com?urlParam=", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeEmptyQueryParams() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("urlParam1", "");
        parameters.add("urlParam2", "");
        compare("http://local.com?urlParam1=&urlParam2=", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeNullQueryParam() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("urlParam", null);
        compare("http://local.com?urlParam", "http://local.com", parameters);
    }

    @Test
    public void shouldInvokeNullQueryParams() {
        LinkedMultiValueMap parameters = new LinkedMultiValueMap();
        parameters.add("urlParam1", null);
        parameters.add("urlParam2", null);
        compare("http://local.com?urlParam1&urlParam2", "http://local.com", parameters);
    }

    private void compare(String expectedUri, String endpointUri, MultiValueMap parameters) {
        ExecutionContext context = mockContext(parameters);
        ReadStream<Buffer> stream = mockStream();
        Handler<ProxyConnection> connectionHandler = mock(Handler.class);
        EndpointResolver.ConnectorEndpoint endpoint = mockResolvedEndpoint(endpointUri);

        invoker.invoke(context, stream, connectionHandler);

        verify(endpoint.getConnector(), times(1))
                .request(argThat(proxyRequest -> expectedUri.equals(proxyRequest.uri())));
    }

    private ExecutionContext mockContext(MultiValueMap multiValueMap) {
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.request()).thenReturn(mock(Request.class));
        when(context.request().parameters()).thenReturn(multiValueMap);
        when(context.request().metrics()).thenReturn(Metrics.on((new Date()).getTime()).build());
        return context;
    }

    private EndpointResolver.ConnectorEndpoint mockResolvedEndpoint(String uri) {
        EndpointResolver.ConnectorEndpoint endpoint = mock(EndpointResolver.ConnectorEndpoint.class);
        when(endpoint.getUri()).thenReturn(uri);
        when(endpoint.getConnector()).thenReturn(mock(Connector.class));
        when(endpoint.getConnector().request(any())).thenReturn(mock(ProxyConnection.class));
        when(endpointResolver.resolve(any())).thenReturn(endpoint);
        return endpoint;
    }

    private ReadStream<Buffer> mockStream() {
        ReadStream<Buffer> stream = mock(ReadStream.class);
        when(stream.bodyHandler(any())).thenReturn(stream);

        return stream;
    }
}

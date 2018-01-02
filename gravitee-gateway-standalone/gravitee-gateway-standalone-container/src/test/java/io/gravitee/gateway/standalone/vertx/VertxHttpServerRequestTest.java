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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * @author Guillaume GILLON (guillaume.gillon at outlook.com)
 */
public class VertxHttpServerRequestTest {

    @Mock
    private HttpServerRequest httpServerRequest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_not_X_Forward_for_in_Header() {
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(8080,"192.168.0.1"));
        when(httpServerRequest.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn(null);


        VertxHttpServerRequest vertxRequest = new VertxHttpServerRequest(httpServerRequest);

        Assert.assertEquals("192.168.0.1", vertxRequest.metrics().getRemoteAddress());
    }

    @Test
    public void test_with_one_X_Forward_for_in_Header() {
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(8080,"192.168.0.1"));
        when(httpServerRequest.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn("197.225.30.74");


        VertxHttpServerRequest vertxRequest = new VertxHttpServerRequest(httpServerRequest);

        Assert.assertEquals("197.225.30.74", vertxRequest.metrics().getRemoteAddress());
    }

    @Test
    public void test_with_many_X_Forward_for_in_Header() {
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(8080,"192.168.0.1"));
        when(httpServerRequest.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn("197.225.30.74, 10.0.0.1, 10.0.0.2");


        VertxHttpServerRequest vertxRequest = new VertxHttpServerRequest(httpServerRequest);

        Assert.assertEquals("197.225.30.74", vertxRequest.metrics().getRemoteAddress());
    }

}

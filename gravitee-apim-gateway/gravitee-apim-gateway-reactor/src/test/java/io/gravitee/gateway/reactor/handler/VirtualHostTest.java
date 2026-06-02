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
package io.gravitee.gateway.reactor.handler;

import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VirtualHostTest {

    @Mock
    private Request request;

    @Test
    public void shouldHaveWeightEqualsTo1() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/");

        Assertions.assertEquals(1, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo5() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/path/to/my/api");

        Assertions.assertEquals(5, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo5_duplicatedPathSeparator() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/path//to///my/api");

        Assertions.assertEquals(5, vHost.priority());
    }

    @Test
    public void shouldHaveWeightEqualsTo1001() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("api.gravitee.io", "/");

        Assertions.assertEquals(1001, vHost.priority());
    }

    @Test
    public void shouldAccept_contextPath() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/");

        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assertions.assertTrue(accept);
    }

    @Test
    public void shouldNotAccept_contextPath() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/products");

        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assertions.assertFalse(accept);
    }

    @Test
    public void shouldAccept_contextPath_subPath() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/products");

        when(request.path()).thenReturn("/products");
        boolean accept = vHost.accept(request);

        Assertions.assertTrue(accept);
    }

    @Test
    public void shouldAccept_contextPath_trailingSlash() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/products");

        when(request.path()).thenReturn("/products/");
        boolean accept = vHost.accept(request);

        Assertions.assertTrue(accept);
    }

    @Test
    public void shouldNotAccept_contextPath_subPath() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("/products");

        when(request.path()).thenReturn("/products2");
        boolean accept = vHost.accept(request);

        Assertions.assertFalse(accept);
    }

    @Test
    public void shouldNotAccept_withHost() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("api.gravitee.io", "/");

        // The request does not contain a HOST header
        when(request.path()).thenReturn("/");
        boolean accept = vHost.accept(request);

        Assertions.assertFalse(accept);
    }

    @Test
    public void shouldAccept_withHost() {
        DefaultHttpAcceptor vHost = new DefaultHttpAcceptor("api.gravitee.io", "/");

        when(request.path()).thenReturn("/");
        when(request.host()).thenReturn("api.gravitee.io");
        boolean accept = vHost.accept(request);

        Assertions.assertTrue(accept);
    }
}

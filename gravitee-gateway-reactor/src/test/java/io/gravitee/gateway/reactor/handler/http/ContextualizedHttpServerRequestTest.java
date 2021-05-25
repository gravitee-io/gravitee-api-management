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
package io.gravitee.gateway.reactor.handler.http;

import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ContextualizedHttpServerRequestTest {

    @Mock
    private Request request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldSplitContextPath() {
        when(request.path()).thenReturn("/path/to/resource");

        Request request = new ContextualizedHttpServerRequest("/", this.request);

        Assert.assertEquals("/path/to/resource", request.pathInfo());
        Assert.assertEquals("/", request.contextPath());
    }

    @Test
    public void shouldSplitContextPath2() {
        when(request.path()).thenReturn("/context-path/path/to/resource");

        Request request = new ContextualizedHttpServerRequest("/context-path/", this.request);

        Assert.assertEquals("/path/to/resource", request.pathInfo());
        Assert.assertEquals("/context-path/", request.contextPath());
    }
}

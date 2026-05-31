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
package io.gravitee.gateway.reactor.handler.http;

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
public class ContextualizedHttpServerRequestTest {

    @Mock
    private Request request;

    @Test
    public void shouldSplitContextPath() {
        when(request.path()).thenReturn("/path/to/resource");

        Request request = new ContextualizedHttpServerRequest("/", this.request);

        Assertions.assertEquals("/path/to/resource", request.pathInfo());
        Assertions.assertEquals("/", request.contextPath());
    }

    @Test
    public void shouldSplitContextPath2() {
        when(request.path()).thenReturn("/context-path/path/to/resource");

        Request request = new ContextualizedHttpServerRequest("/context-path/", this.request);

        Assertions.assertEquals("/path/to/resource", request.pathInfo());
        Assertions.assertEquals("/context-path/", request.contextPath());
    }
}

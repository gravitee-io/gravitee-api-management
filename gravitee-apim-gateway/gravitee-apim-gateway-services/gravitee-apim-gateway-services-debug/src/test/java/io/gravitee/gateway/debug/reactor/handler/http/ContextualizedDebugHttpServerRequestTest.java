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
package io.gravitee.gateway.debug.reactor.handler.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.Request;
import org.junit.Test;

public class ContextualizedDebugHttpServerRequestTest {

    private static final String EVENT_ID = "my-event-id";
    private static final String DEBUG_CONTEXT_PATH = String.format("/%s-echo", EVENT_ID);

    @Test
    public void shouldRemoveEventIdFromContextPathAndPath() {
        final Request request = mock(Request.class);
        when(request.path()).thenReturn(String.format("%s/another/path", DEBUG_CONTEXT_PATH));

        final ContextualizedDebugHttpServerRequest contextualizedRequest = new ContextualizedDebugHttpServerRequest(
            DEBUG_CONTEXT_PATH,
            request,
            EVENT_ID
        );
        assertThat(contextualizedRequest.contextPath()).isEqualTo("/echo");
        assertThat(contextualizedRequest.path()).isEqualTo("/echo/another/path");
    }
}

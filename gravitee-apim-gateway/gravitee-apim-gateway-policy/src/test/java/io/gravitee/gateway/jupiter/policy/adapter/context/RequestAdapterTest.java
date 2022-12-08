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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.jupiter.http.vertx.VertxHttpServerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class RequestAdapterTest {

    @Mock
    private VertxHttpServerRequest request;

    @Mock
    private Runnable onResumeHandler;

    @Mock
    private Handler<Buffer> bodyHandler;

    @Mock
    private Handler<Void> endHandler;

    private RequestAdapter cut;

    @BeforeEach
    void init() {
        cut = new RequestAdapter(request, null);
        cut.onResume(onResumeHandler);
        cut.bodyHandler(bodyHandler);
        cut.endHandler(endHandler);
    }

    @Test
    void shouldCallOnResumeHandlerWhenResumeForTheFirstTime() {
        cut.resume();
        verify(onResumeHandler).run();
    }

    @Test
    void shouldResumeRequest() {
        // First resume calls onResume handler, any other resumes the request.
        for (int i = 0; i < 10; i++) {
            cut.resume();
        }

        verify(request, times(9)).resume();
    }

    @Test
    void shouldPauseRequest() {
        for (int i = 0; i < 10; i++) {
            cut.pause();
        }

        verify(request, times(10)).pause();
    }
}

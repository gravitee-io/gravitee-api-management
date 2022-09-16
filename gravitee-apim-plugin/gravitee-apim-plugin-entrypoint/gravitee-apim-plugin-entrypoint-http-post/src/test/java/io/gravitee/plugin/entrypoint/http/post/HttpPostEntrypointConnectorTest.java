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
package io.gravitee.plugin.entrypoint.http.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpPostEntrypointConnectorTest {

    @Mock
    private ExecutionContext mockExecutionContext;

    private HttpPostEntrypointConnector httpPostEntrypointConnector;
    private DummyMessageRequest dummyMessageRequest;

    @BeforeEach
    void beforeEach() {
        dummyMessageRequest = new DummyMessageRequest();
        lenient().when(mockExecutionContext.request()).thenReturn(dummyMessageRequest);
        httpPostEntrypointConnector = new HttpPostEntrypointConnector(null);
    }

    @Test
    void shouldIdReturnHttpPost() {
        assertThat(httpPostEntrypointConnector.id()).isEqualTo("http-post");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(httpPostEntrypointConnector.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(httpPostEntrypointConnector.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportPublishModeOnly() {
        assertThat(httpPostEntrypointConnector.supportedModes()).containsOnly(ConnectorMode.PUBLISH);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(httpPostEntrypointConnector.matchCriteriaCount()).isEqualTo(1);
    }

    @Test
    void shouldMatchesWithValidContext() {
        dummyMessageRequest.method(HttpMethod.POST);

        boolean matches = httpPostEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        dummyMessageRequest.method(HttpMethod.GET);

        boolean matches = httpPostEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteAndSetRequestBuffersInRequestMessages() {
        dummyMessageRequest.headers(HttpHeaders.create());
        dummyMessageRequest.body(Maybe.just(Buffer.buffer("bodyContent")));
        httpPostEntrypointConnector.handleRequest(mockExecutionContext).test().assertComplete();

        dummyMessageRequest
            .messages()
            .test()
            .assertValue(message -> "bodyContent".equals(message.content().toString()) && message.id() != null);
    }

    @Test
    void shouldCompleteWhenResponseMessagesComplete() {
        dummyMessageRequest.messages(Flowable.empty());
        httpPostEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();
    }
}

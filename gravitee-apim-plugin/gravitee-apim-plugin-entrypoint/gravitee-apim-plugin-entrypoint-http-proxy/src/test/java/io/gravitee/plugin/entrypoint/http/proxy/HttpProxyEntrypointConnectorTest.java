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
package io.gravitee.plugin.entrypoint.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
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
class HttpProxyEntrypointConnectorTest {

    @Mock
    private HttpExecutionContext ctx;

    private HttpProxyEntrypointConnector cut;

    @BeforeEach
    void init() {
        cut = new HttpProxyEntrypointConnector(null);
    }

    @Test
    void shouldIdReturnHttpProxy() {
        assertThat(cut.id()).isEqualTo("http-proxy");
    }

    @Test
    void shouldSupportSyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportRequestResponseModeOnly() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.REQUEST_RESPONSE);
    }

    @Test
    void shouldMatchesCriteriaReturnMinValue() {
        assertThat(cut.matchCriteriaCount()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void shouldAlwaysMatch() {
        assertThat(cut.matches(ctx)).isTrue();
    }

    @Test
    void shouldCompleteImmediatelyWhenHandleRequest() {
        cut.handleRequest(ctx).test().assertComplete();
    }

    @Test
    void shouldCompleteImmediatelyWhenHandleResponse() {
        cut.handleResponse(ctx).test().assertComplete();
    }
}

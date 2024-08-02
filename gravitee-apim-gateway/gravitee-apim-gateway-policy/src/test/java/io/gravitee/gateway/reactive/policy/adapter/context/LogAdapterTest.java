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
package io.gravitee.gateway.reactive.policy.adapter.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.v4.log.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LogAdapterTest {

    @Mock
    private Log logV4;

    private LogAdapter logAdapter;

    @BeforeEach
    public void beforeEach() {
        logAdapter = new LogAdapter(logV4);
    }

    @Test
    void should_delegate_getApi_to_log_v4() {
        // When
        logAdapter.getApi();

        // Then
        verify(logV4).getApiId();
    }

    @Test
    void should_delegate_getApiName_to_log_v4() {
        // When
        logAdapter.getApiName();

        // Then
        verify(logV4).getApiName();
    }

    @Test
    void should_delegate_setApi_to_log_v4() {
        // When
        logAdapter.setApi(null);

        // Then
        verify(logV4).setApiId(any());
    }

    @Test
    void should_delegate_setApiName_to_log_v4() {
        // When
        logAdapter.setApiName(null);

        // Then
        verify(logV4).setApiName(any());
    }

    @Test
    void should_delegate_getRequestId_to_log_v4() {
        // When
        logAdapter.getRequestId();

        // Then
        verify(logV4).getRequestId();
    }

    @Test
    void should_delegate_setRequestId_to_log_v4() {
        // When
        logAdapter.setRequestId(null);

        // Then
        verify(logV4).setRequestId(any());
    }

    @Test
    void should_delegate_getClientRequest_to_log_v4() {
        // When
        logAdapter.getClientRequest();

        // Then
        verify(logV4).getEntrypointRequest();
    }

    @Test
    void should_delegate_setClientRequest_to_log_v4() {
        // When
        logAdapter.setClientRequest(null);

        // Then
        verify(logV4).setEntrypointRequest(any());
    }

    @Test
    void should_delegate_getProxyRequest_to_log_v4() {
        // When
        logAdapter.getProxyRequest();

        // Then
        verify(logV4).getEndpointRequest();
    }

    @Test
    void should_delegate_setProxyRequest_to_log_v4() {
        // When
        logAdapter.setProxyRequest(null);

        // Then
        verify(logV4).setEndpointRequest(any());
    }

    @Test
    void should_delegate_getClientResponse_to_log_v4() {
        // When
        logAdapter.getClientResponse();

        // Then
        verify(logV4).getEntrypointResponse();
    }

    @Test
    void should_delegate_setClientResponse_to_log_v4() {
        // When
        logAdapter.setClientResponse(null);

        // Then
        verify(logV4).setEntrypointResponse(any());
    }

    @Test
    void should_delegate_getProxyResponse_to_log_v4() {
        // When
        logAdapter.getProxyResponse();

        // Then
        verify(logV4).getEndpointResponse();
    }

    @Test
    void should_delegate_setProxyResponse_to_log_v4() {
        // When
        logAdapter.setProxyResponse(null);

        // Then
        verify(logV4).setEndpointResponse(any());
    }
}

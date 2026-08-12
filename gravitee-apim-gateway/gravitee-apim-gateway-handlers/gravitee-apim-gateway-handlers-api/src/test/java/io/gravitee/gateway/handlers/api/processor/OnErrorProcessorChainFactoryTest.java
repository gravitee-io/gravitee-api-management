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
package io.gravitee.gateway.handlers.api.processor;

import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_REQUEST_ID_HEADER;
import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.transaction.TransactionResponseProcessorConfiguration;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.http.Metrics;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OnErrorProcessorChainFactoryTest {

    private static final String TRANSACTION_ID = "transaction-id";
    private static final String REQUEST_ID = "request-id";

    private OnErrorProcessorChainFactory factory;

    @BeforeEach
    void setUp() {
        Configuration nodeConfiguration = mock(Configuration.class);
        when(nodeConfiguration.getProperty(eq("handlers.request.transaction.header"), anyString())).thenReturn(
            DEFAULT_TRANSACTION_ID_HEADER
        );
        when(nodeConfiguration.getProperty(eq("handlers.request.request.header"), anyString())).thenReturn(DEFAULT_REQUEST_ID_HEADER);

        factory = new OnErrorProcessorChainFactory(
            buildApi(),
            mock(PolicyChainFactory.class),
            new TransactionResponseProcessorConfiguration(nodeConfiguration)
        );
    }

    @Test
    @DisplayName("Error response carries transaction and request id headers from the request")
    void should_set_transaction_and_request_id_headers_on_error_response() {
        Request request = mock(Request.class);
        when(request.metrics()).thenReturn(Metrics.on(0).build());
        HttpHeaders requestHeaders = HttpHeaders.create();
        requestHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, TRANSACTION_ID);
        requestHeaders.set(DEFAULT_REQUEST_ID_HEADER, REQUEST_ID);
        when(request.headers()).thenReturn(requestHeaders);

        Response response = mock(Response.class);
        HttpHeaders responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);

        ExecutionContext context = new SimpleExecutionContext(request, response);
        context.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, mockFailure());

        createAndHandleChain(context);

        assertEquals(List.of(TRANSACTION_ID), responseHeaders.getAll(DEFAULT_TRANSACTION_ID_HEADER));
        assertEquals(List.of(REQUEST_ID), responseHeaders.getAll(DEFAULT_REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("Error response overrides backend transaction and request id headers")
    void should_override_backend_transaction_and_request_id_headers_on_error_response() {
        Request request = mock(Request.class);
        when(request.metrics()).thenReturn(Metrics.on(0).build());
        HttpHeaders requestHeaders = HttpHeaders.create();
        requestHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, TRANSACTION_ID);
        requestHeaders.set(DEFAULT_REQUEST_ID_HEADER, REQUEST_ID);
        when(request.headers()).thenReturn(requestHeaders);

        Response response = mock(Response.class);
        HttpHeaders responseHeaders = HttpHeaders.create();
        responseHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        responseHeaders.set(DEFAULT_REQUEST_ID_HEADER, "backend-request-id");
        when(response.headers()).thenReturn(responseHeaders);

        ExecutionContext context = new SimpleExecutionContext(request, response);
        context.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, mockFailure());

        createAndHandleChain(context);

        assertEquals(List.of(TRANSACTION_ID), responseHeaders.getAll(DEFAULT_TRANSACTION_ID_HEADER));
        assertEquals(List.of(REQUEST_ID), responseHeaders.getAll(DEFAULT_REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("Error response keeps backend transaction and request id headers in KEEP override mode")
    void should_keep_backend_transaction_and_request_id_headers_in_keep_override_mode() {
        OnErrorProcessorChainFactory keepFactory = buildFactory("KEEP");

        Request request = mock(Request.class);
        when(request.metrics()).thenReturn(Metrics.on(0).build());
        HttpHeaders requestHeaders = HttpHeaders.create();
        requestHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, TRANSACTION_ID);
        requestHeaders.set(DEFAULT_REQUEST_ID_HEADER, REQUEST_ID);
        when(request.headers()).thenReturn(requestHeaders);

        Response response = mock(Response.class);
        HttpHeaders responseHeaders = HttpHeaders.create();
        responseHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        responseHeaders.set(DEFAULT_REQUEST_ID_HEADER, "backend-request-id");
        when(response.headers()).thenReturn(responseHeaders);

        ExecutionContext context = new SimpleExecutionContext(request, response);
        context.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, mockFailure());

        createAndHandleChain(keepFactory, context);

        assertEquals(List.of("backend-transaction-id"), responseHeaders.getAll(DEFAULT_TRANSACTION_ID_HEADER));
        assertEquals(List.of("backend-request-id"), responseHeaders.getAll(DEFAULT_REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("Error response merges transaction and request id headers in MERGE override mode")
    void should_merge_transaction_and_request_id_headers_in_merge_override_mode() {
        OnErrorProcessorChainFactory mergeFactory = buildFactory("MERGE");

        Request request = mock(Request.class);
        when(request.metrics()).thenReturn(Metrics.on(0).build());
        HttpHeaders requestHeaders = HttpHeaders.create();
        requestHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, TRANSACTION_ID);
        requestHeaders.set(DEFAULT_REQUEST_ID_HEADER, REQUEST_ID);
        when(request.headers()).thenReturn(requestHeaders);

        Response response = mock(Response.class);
        HttpHeaders responseHeaders = HttpHeaders.create();
        responseHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        responseHeaders.set(DEFAULT_REQUEST_ID_HEADER, "backend-request-id");
        when(response.headers()).thenReturn(responseHeaders);

        ExecutionContext context = new SimpleExecutionContext(request, response);
        context.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, mockFailure());

        createAndHandleChain(mergeFactory, context);

        assertEquals(List.of("backend-transaction-id", TRANSACTION_ID), responseHeaders.getAll(DEFAULT_TRANSACTION_ID_HEADER));
        assertEquals(List.of("backend-request-id", REQUEST_ID), responseHeaders.getAll(DEFAULT_REQUEST_ID_HEADER));
    }

    private OnErrorProcessorChainFactory buildFactory(String overrideMode) {
        Configuration config = mock(Configuration.class);
        when(config.getProperty(eq("handlers.request.transaction.header"), anyString())).thenReturn(DEFAULT_TRANSACTION_ID_HEADER);
        when(config.getProperty(eq("handlers.request.request.header"), anyString())).thenReturn(DEFAULT_REQUEST_ID_HEADER);
        if (overrideMode != null) {
            when(config.getProperty(eq("handlers.request.transaction.overrideMode"))).thenReturn(overrideMode);
            when(config.getProperty(eq("handlers.request.request.overrideMode"))).thenReturn(overrideMode);
        }
        return new OnErrorProcessorChainFactory(
            buildApi(),
            mock(PolicyChainFactory.class),
            new TransactionResponseProcessorConfiguration(config)
        );
    }

    private ProcessorFailure mockFailure() {
        ProcessorFailure failure = mock(ProcessorFailure.class);
        when(failure.statusCode()).thenReturn(401);
        when(failure.key()).thenReturn("UNAUTHORIZED");
        when(failure.message()).thenReturn("Unauthorized");
        return failure;
    }

    private void createAndHandleChain(ExecutionContext context) {
        createAndHandleChain(factory, context);
    }

    private void createAndHandleChain(OnErrorProcessorChainFactory chainFactory, ExecutionContext context) {
        chainFactory
            .create()
            .handler(__ -> {})
            .errorHandler(failure -> {
                throw new AssertionError(failure);
            })
            .exitHandler(__ -> {})
            .handle(context);
    }

    private Api buildApi() {
        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        definition.setProxy(new Proxy());
        return new Api(definition);
    }
}

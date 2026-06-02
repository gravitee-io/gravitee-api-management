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
package io.gravitee.gateway.reactor.processor.transaction;

import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_REQUEST_ID_HEADER;
import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.MultiMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransactionRequestProcessorTest {

    private static final String CUSTOM_TRANSACTION_ID_HEADER = "X-My-Transaction-Id";
    private static final String CUSTOM_REQUEST_ID_HEADER = "X-My-Request-Id";

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @BeforeEach
    public void setUp() throws Exception {
        context = new SimpleExecutionContext(request, response);
        Mockito.when(request.id()).thenReturn(UUID.random().toString());
        Mockito.when(request.headers()).thenReturn(HttpHeaders.create());
        Mockito.when(request.parameters()).thenReturn(new VertxHttpHeaders(MultiMap.caseInsensitiveMultiMap()));
        Mockito.when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void shouldHaveTransactionId() {
        Mockito.when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionRequestProcessor().handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(context.request().transactionId(), context.request().headers().get(DEFAULT_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(context.request().transactionId(), context.request().metrics().getTransactionId());
        Assertions.assertEquals(context.request().id(), context.request().headers().get(DEFAULT_REQUEST_ID_HEADER));
    }

    @Test
    public void shouldPropagateSameTransactionId() {
        String transactionId = UUID.toString(UUID.random());

        request.headers().set(DEFAULT_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor().handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().headers().get(DEFAULT_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(transactionId, context.request().metrics().getTransactionId());

        Assertions.assertNotEquals(transactionId, context.request().id());
        Assertions.assertNotEquals(transactionId, context.request().headers().get(DEFAULT_REQUEST_ID_HEADER));
        Assertions.assertNotEquals(transactionId, context.request().metrics().getRequestId());
    }

    @Test
    public void shouldExtractTransactionIdFromDefaultParam() {
        String transactionId = UUID.toString(UUID.random());

        request.parameters().set(DEFAULT_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor().handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().headers().get(DEFAULT_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(transactionId, context.request().metrics().getTransactionId());
    }

    @Test
    public void shouldHaveTransactionIdWithCustomHeader() {
        Mockito.when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionRequestProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER).handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(context.request().transactionId(), context.request().headers().get(CUSTOM_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(context.request().transactionId(), context.request().metrics().getTransactionId());
    }

    @Test
    public void shouldPropagateSameTransactionIdWithCustomHeader() {
        String transactionId = UUID.toString(UUID.random());

        request.headers().set(CUSTOM_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER).handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().headers().get(CUSTOM_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(transactionId, context.request().metrics().getTransactionId());
    }

    @Test
    public void shouldExtractTransactionIdFromCustomParam() {
        String transactionId = UUID.toString(UUID.random());

        request.parameters().set(CUSTOM_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER).handler(context -> {}).handle(context);

        Assertions.assertNotNull(context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().transactionId());
        Assertions.assertEquals(transactionId, context.request().headers().get(CUSTOM_TRANSACTION_ID_HEADER));
        Assertions.assertEquals(transactionId, context.request().metrics().getTransactionId());
    }
}

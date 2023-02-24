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
package io.gravitee.gateway.reactor.processor.transaction;

import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_REQUEST_ID_HEADER;
import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionRequestProcessorTest {

    private static final String CUSTOM_TRANSACTION_ID_HEADER = "X-My-Transaction-Id";
    private static final String CUSTOM_REQUEST_ID_HEADER = "X-My-Request-Id";

    private MutableExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new SimpleExecutionContext(request, response);
        Mockito.when(request.headers()).thenReturn(HttpHeaders.create());
        Mockito.when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void shouldHaveTransactionId() throws InterruptedException {
        Mockito.when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionRequestProcessor().handler(context -> {}).handle(context);

        Assert.assertNotNull(context.request().transactionId());
        Assert.assertEquals(context.request().transactionId(), context.request().headers().getFirst(DEFAULT_TRANSACTION_ID_HEADER));
        Assert.assertEquals(context.request().transactionId(), context.request().metrics().getTransactionId());
        Assert.assertEquals(context.request().id(), context.request().headers().getFirst(DEFAULT_REQUEST_ID_HEADER));
    }

    @Test
    public void shouldPropagateSameTransactionId() {
        String transactionId = UUID.toString(UUID.random());

        request.headers().set(DEFAULT_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor().handler(context -> {}).handle(context);

        Assert.assertNotNull(context.request().transactionId());
        Assert.assertEquals(transactionId, context.request().transactionId());
        Assert.assertEquals(transactionId, context.request().headers().get(DEFAULT_TRANSACTION_ID_HEADER));
        Assert.assertEquals(transactionId, context.request().metrics().getTransactionId());

        Assert.assertNotEquals(transactionId, context.request().id());
        Assert.assertNotEquals(transactionId, context.request().headers().get(DEFAULT_REQUEST_ID_HEADER));
        Assert.assertNotEquals(transactionId, context.request().metrics().getRequestId());
    }

    @Test
    public void shouldHaveTransactionIdWithCustomHeader() {
        Mockito.when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionRequestProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER).handler(context -> {}).handle(context);

        Assert.assertNotNull(context.request().transactionId());
        Assert.assertEquals(context.request().transactionId(), context.request().headers().get(CUSTOM_TRANSACTION_ID_HEADER));
        Assert.assertEquals(context.request().transactionId(), context.request().metrics().getTransactionId());
    }

    @Test
    public void shouldPropagateSameTransactionIdWithCustomHeader() throws InterruptedException {
        String transactionId = UUID.toString(UUID.random());

        request.headers().set(CUSTOM_TRANSACTION_ID_HEADER, transactionId);
        new TransactionRequestProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER).handler(context -> {}).handle(context);

        Assert.assertNotNull(context.request().transactionId());
        Assert.assertEquals(transactionId, context.request().transactionId());
        Assert.assertEquals(transactionId, context.request().headers().get(CUSTOM_TRANSACTION_ID_HEADER));
        Assert.assertEquals(transactionId, context.request().metrics().getTransactionId());
    }
}

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
package io.gravitee.gateway.reactor.handler.transaction;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TransactionHandlerTest {

    private final static String CUSTOM_TRANSACTION_ID_HEADER = "X-My-Transaction-Id";

    @Mock
    private Request request;
    @Mock
    private Response response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(request.headers()).thenReturn(new HttpHeaders());
        when(response.headers()).thenReturn(new HttpHeaders());
        when(request.metrics()).thenReturn(Metrics.on(System.currentTimeMillis()).build());
    }

    @Test
    public void shouldHaveTransactionId() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionHandler(request1 -> {
            assertNotNull(request1.transactionId());
            assertEquals(request1.transactionId(), request1.headers().getFirst(TransactionHandler.DEFAULT_TRANSACTIONAL_ID_HEADER));
            assertEquals(request1.transactionId(), request1.metrics().getTransactionId());
            assertEquals(request1.transactionId(), response.headers().getFirst(TransactionHandler.DEFAULT_TRANSACTIONAL_ID_HEADER));
            lock.countDown();
        },response).handle(request);

        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldPropagateSameTransactionId() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        String transactionId = UUID.toString(UUID.random());

        request.headers().set(TransactionHandler.DEFAULT_TRANSACTIONAL_ID_HEADER, transactionId);
        new TransactionHandler(request1 -> {
            assertNotNull(request1.transactionId());
            assertEquals(transactionId, request1.transactionId());
            assertEquals(transactionId, request1.headers().getFirst(TransactionHandler.DEFAULT_TRANSACTIONAL_ID_HEADER));
            assertEquals(transactionId, request1.metrics().getTransactionId());
            assertEquals(request1.transactionId(), response.headers().getFirst(TransactionHandler.DEFAULT_TRANSACTIONAL_ID_HEADER));
            lock.countDown();
        },response).handle(request);

        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldHaveTransactionIdWithCustomHeader() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        when(request.id()).thenReturn(UUID.toString(UUID.random()));

        new TransactionHandler(CUSTOM_TRANSACTION_ID_HEADER, request1 -> {
            assertNotNull(request1.transactionId());
            assertEquals(request1.transactionId(), request1.headers().getFirst(CUSTOM_TRANSACTION_ID_HEADER));
            assertEquals(request1.transactionId(), request1.metrics().getTransactionId());
            assertEquals(request1.transactionId(), response.headers().getFirst(CUSTOM_TRANSACTION_ID_HEADER));
            lock.countDown();
        },response).handle(request);

        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldPropagateSameTransactionIdWithCustomHeader() throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        String transactionId = UUID.toString(UUID.random());

        request.headers().set(CUSTOM_TRANSACTION_ID_HEADER, transactionId);
        new TransactionHandler(CUSTOM_TRANSACTION_ID_HEADER, request1 -> {
            assertNotNull(request1.transactionId());
            assertEquals(transactionId, request1.transactionId());
            assertEquals(transactionId, request1.headers().getFirst(CUSTOM_TRANSACTION_ID_HEADER));
            assertEquals(transactionId, request1.metrics().getTransactionId());
            assertEquals(request1.transactionId(), response.headers().getFirst(CUSTOM_TRANSACTION_ID_HEADER));
            lock.countDown();
        },response).handle(request);

        assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }
}

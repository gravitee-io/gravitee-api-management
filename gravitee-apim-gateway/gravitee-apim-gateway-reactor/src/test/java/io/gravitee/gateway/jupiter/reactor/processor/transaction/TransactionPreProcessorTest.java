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
package io.gravitee.gateway.jupiter.reactor.processor.transaction;

import static io.gravitee.gateway.jupiter.reactor.processor.transaction.TransactionHeader.DEFAULT_REQUEST_ID_HEADER;
import static io.gravitee.gateway.jupiter.reactor.processor.transaction.TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.jupiter.reactor.processor.AbstractProcessorTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class TransactionPreProcessorTest extends AbstractProcessorTest {

    private static final String CUSTOM_TRANSACTION_ID_HEADER = "X-My-Transaction-Id";
    private static final String CUSTOM_REQUEST_ID_HEADER = "X-My-Request-Id";
    private TransactionPreProcessor transactionProcessor;

    @BeforeEach
    public void setUp() throws Exception {
        transactionProcessor = new TransactionPreProcessor(DEFAULT_TRANSACTION_ID_HEADER, DEFAULT_REQUEST_ID_HEADER);
    }

    @Test
    void shouldHaveTransactionId() {
        String requestId = UUID.toString(UUID.random());
        when(mockRequest.id()).thenReturn(requestId);
        transactionProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).transactionId(requestId);
        assertThat(spyRequestHeaders.get(DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(requestId);
        assertThat(spyRequestHeaders.get(DEFAULT_REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(metrics.getTransactionId()).isEqualTo(requestId);
    }

    @Test
    void shouldPropagateSameTransactionId() {
        String transactionId = UUID.toString(UUID.random());
        String requestId = UUID.toString(UUID.random());
        when(mockRequest.id()).thenReturn(requestId);
        spyRequestHeaders.set(DEFAULT_TRANSACTION_ID_HEADER, transactionId);
        transactionProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).transactionId(transactionId);
        assertThat(spyRequestHeaders.get(DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(transactionId);
        assertThat(spyRequestHeaders.get(DEFAULT_REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(metrics.getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    void shouldHaveTransactionIdWithCustomHeader() {
        transactionProcessor = new TransactionPreProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER);
        String requestId = UUID.toString(UUID.random());
        when(mockRequest.id()).thenReturn(requestId);
        transactionProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).transactionId(requestId);
        assertThat(spyRequestHeaders.get(CUSTOM_TRANSACTION_ID_HEADER)).isEqualTo(requestId);
        assertThat(spyRequestHeaders.get(DEFAULT_TRANSACTION_ID_HEADER)).isNull();
        assertThat(spyRequestHeaders.get(CUSTOM_REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(spyRequestHeaders.get(DEFAULT_REQUEST_ID_HEADER)).isNull();
        assertThat(metrics.getTransactionId()).isEqualTo(requestId);
        assertThat(spyRequestHeaders.get(DEFAULT_TRANSACTION_ID_HEADER)).isNull();
        assertThat(spyRequestHeaders.get(DEFAULT_REQUEST_ID_HEADER)).isNull();
    }

    @Test
    void shouldPropagateSameTransactionIdWithCustomHeader() {
        transactionProcessor = new TransactionPreProcessor(CUSTOM_TRANSACTION_ID_HEADER, CUSTOM_REQUEST_ID_HEADER);
        String transactionId = UUID.toString(UUID.random());
        String requestId = UUID.toString(UUID.random());
        when(mockRequest.id()).thenReturn(requestId);
        spyRequestHeaders.set(CUSTOM_TRANSACTION_ID_HEADER, transactionId);
        transactionProcessor.execute(ctx).test().assertResult();
        verify(mockRequest).transactionId(transactionId);
        assertThat(spyRequestHeaders.get(CUSTOM_TRANSACTION_ID_HEADER)).isEqualTo(transactionId);
        assertThat(spyRequestHeaders.get(CUSTOM_REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(metrics.getTransactionId()).isEqualTo(transactionId);
    }
}

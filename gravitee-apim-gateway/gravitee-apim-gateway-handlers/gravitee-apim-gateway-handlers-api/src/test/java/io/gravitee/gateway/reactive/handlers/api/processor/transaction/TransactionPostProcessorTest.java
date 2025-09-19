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
package io.gravitee.gateway.reactive.handlers.api.processor.transaction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.gateway.reactor.processor.transaction.TransactionHeader;
import io.gravitee.node.api.configuration.Configuration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionPostProcessorTest extends AbstractProcessorTest {

    private TransactionPostProcessor transactionPostProcessor;

    @Test
    @DisplayName("By default should override Transaction Id and Request Id headers set by the backend, by the ones set by APIM")
    void handleWithOverrideByDefault() {
        Configuration nodeConfiguration = mock(Configuration.class);
        when(nodeConfiguration.getProperty("handlers.request.transaction.overrideMode")).thenReturn(null);
        when(nodeConfiguration.getProperty(eq("handlers.request.transaction.header"), anyString())).thenReturn(
            TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER
        );
        when(nodeConfiguration.getProperty("handlers.request.request.overrideMode")).thenReturn(null);
        when(nodeConfiguration.getProperty(eq("handlers.request.request.header"), anyString())).thenReturn(
            TransactionHeader.DEFAULT_REQUEST_ID_HEADER
        );

        TransactionPostProcessorConfiguration processorConfiguration = new TransactionPostProcessorConfiguration(nodeConfiguration);

        transactionPostProcessor = new TransactionPostProcessor(processorConfiguration);

        spyCtx.request().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "transaction-id");
        spyCtx.request().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "request-id");

        spyCtx.response().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        spyCtx.response().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "backend-request-id");

        transactionPostProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(List.of("transaction-id"));
        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_REQUEST_ID_HEADER)).isEqualTo(List.of("request-id"));
    }

    @Test
    @DisplayName("Should override Transaction Id and Request Id headers set by the backend, by the ones set by APIM")
    void handleWithOverrideBackendModeNone() {
        instantiateTransactionPostProcessor(TransactionHeaderOverrideMode.OVERRIDE, TransactionHeaderOverrideMode.OVERRIDE);

        spyCtx.request().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "transaction-id");
        spyCtx.request().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "request-id");

        spyCtx.response().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        spyCtx.response().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "backend-request-id");

        transactionPostProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(List.of("transaction-id"));
        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_REQUEST_ID_HEADER)).isEqualTo(List.of("request-id"));
    }

    @Test
    @DisplayName("Should merge transaction ID and request ID headers set by APIM and the backend")
    void handleWithOverrideBackendModeMerge() {
        instantiateTransactionPostProcessor(TransactionHeaderOverrideMode.MERGE, TransactionHeaderOverrideMode.MERGE);

        spyCtx.request().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "transaction-id");
        spyCtx.request().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "request-id");

        spyCtx.response().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        spyCtx.response().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "backend-request-id");

        transactionPostProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(
            List.of("backend-transaction-id", "transaction-id")
        );
        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_REQUEST_ID_HEADER)).isEqualTo(
            List.of("backend-request-id", "request-id")
        );
    }

    @Test
    @DisplayName("Should keep transaction ID and request ID headers set by the backend, discard APIM ones")
    void handleWithOverrideBackendModeOverride() {
        instantiateTransactionPostProcessor(TransactionHeaderOverrideMode.KEEP, TransactionHeaderOverrideMode.KEEP);

        spyCtx.request().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "transaction-id");
        spyCtx.request().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "request-id");

        spyCtx.response().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        spyCtx.response().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "backend-request-id");

        transactionPostProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(List.of("backend-transaction-id"));
        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_REQUEST_ID_HEADER)).isEqualTo(List.of("backend-request-id"));
    }

    @Test
    @DisplayName("Should override Transaction Id header's value but not Request Id header because empty, by the ones set by APIM")
    void handleWithOverrideWithoutHeaderValue() {
        instantiateTransactionPostProcessor(TransactionHeaderOverrideMode.OVERRIDE, TransactionHeaderOverrideMode.OVERRIDE);

        spyCtx.request().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "transaction-id");

        spyCtx.response().headers().set(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER, "backend-transaction-id");
        spyCtx.response().headers().set(TransactionHeader.DEFAULT_REQUEST_ID_HEADER, "backend-request-id");

        transactionPostProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER)).isEqualTo(List.of("transaction-id"));
        assertThat(spyResponseHeaders.getAll(TransactionHeader.DEFAULT_REQUEST_ID_HEADER)).isEqualTo(List.of("backend-request-id"));
    }

    private void instantiateTransactionPostProcessor(
        TransactionHeaderOverrideMode transactionOverrideMode,
        TransactionHeaderOverrideMode requestHeaderOverrideMode
    ) {
        Configuration nodeConfiguration = mock(Configuration.class);
        when(nodeConfiguration.getProperty("handlers.request.transaction.overrideMode")).thenReturn(transactionOverrideMode.name());
        when(nodeConfiguration.getProperty(eq("handlers.request.transaction.header"), anyString())).thenReturn(
            TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER
        );

        when(nodeConfiguration.getProperty("handlers.request.request.overrideMode")).thenReturn(requestHeaderOverrideMode.name());
        when(nodeConfiguration.getProperty(eq("handlers.request.request.header"), anyString())).thenReturn(
            TransactionHeader.DEFAULT_REQUEST_ID_HEADER
        );

        TransactionPostProcessorConfiguration processorConfiguration = new TransactionPostProcessorConfiguration(nodeConfiguration);

        transactionPostProcessor = new TransactionPostProcessor(processorConfiguration);
    }
}

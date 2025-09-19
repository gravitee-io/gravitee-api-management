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

import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_REQUEST_ID_HEADER;
import static io.gravitee.gateway.reactor.processor.transaction.TransactionHeader.DEFAULT_TRANSACTION_ID_HEADER;

import io.gravitee.node.api.configuration.Configuration;

public class TransactionPostProcessorConfiguration {

    public final String transactionHeader;

    public final TransactionHeaderOverrideMode transactionHeaderHeaderOverrideMode;

    public final String requestHeader;

    public final TransactionHeaderOverrideMode requestHeaderHeaderOverrideMode;

    public TransactionPostProcessorConfiguration(Configuration configuration) {
        this.transactionHeader = configuration.getProperty("handlers.request.transaction.header", DEFAULT_TRANSACTION_ID_HEADER);
        String transactionOverrideModeString = configuration.getProperty("handlers.request.transaction.overrideMode");
        this.transactionHeaderHeaderOverrideMode = transactionOverrideModeString != null
            ? TransactionHeaderOverrideMode.valueOf(transactionOverrideModeString.trim().toUpperCase())
            : TransactionHeaderOverrideMode.OVERRIDE;

        this.requestHeader = configuration.getProperty("handlers.request.request.header", DEFAULT_REQUEST_ID_HEADER);
        String requestOverrideModeString = configuration.getProperty("handlers.request.request.overrideMode");
        this.requestHeaderHeaderOverrideMode = requestOverrideModeString != null
            ? TransactionHeaderOverrideMode.valueOf(requestOverrideModeString.trim().toUpperCase())
            : TransactionHeaderOverrideMode.OVERRIDE;
    }
}

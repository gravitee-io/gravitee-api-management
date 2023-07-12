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
package io.gravitee.gateway.handlers.api.processor.transaction;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.core.processor.AbstractProcessor;

/**
 * A {@link Response} processor used to set the transaction ID in the response headers.
 */
public class TransactionResponseProcessor extends AbstractProcessor<ExecutionContext> {

    private final TransactionResponseProcessorConfiguration configuration;

    public TransactionResponseProcessor(TransactionResponseProcessorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(final ExecutionContext context) {
        setHeaderAccordingToBackendOverrideMode(
            context.request().headers(),
            context.response().headers(),
            configuration.transactionHeader,
            configuration.transactionHeaderHeaderOverrideMode
        );

        setHeaderAccordingToBackendOverrideMode(
            context.request().headers(),
            context.response().headers(),
            configuration.requestHeader,
            configuration.requestHeaderHeaderOverrideMode
        );

        next.handle(context);
    }

    private void setHeaderAccordingToBackendOverrideMode(
        final HttpHeaders requestHeaders,
        final HttpHeaders responseHeaders,
        final String headerName,
        final TransactionHeaderOverrideMode headerOverrideMode
    ) {
        String requestHeaderValue = requestHeaders.get(headerName);
        String backendHeaderValue = responseHeaders.get(headerName);

        if (headerOverrideMode == TransactionHeaderOverrideMode.OVERRIDE) {
            responseHeaders.set(headerName, requestHeaderValue);
        } else if (headerOverrideMode == TransactionHeaderOverrideMode.MERGE) {
            if (requestHeaderValue != null && !requestHeaderValue.equals(backendHeaderValue)) {
                responseHeaders.add(headerName, requestHeaderValue);
            }
        } else if (headerOverrideMode == TransactionHeaderOverrideMode.KEEP) {
            responseHeaders.set(headerName, backendHeaderValue);
        }
    }
}

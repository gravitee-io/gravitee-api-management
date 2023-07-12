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
package io.gravitee.gateway.jupiter.handlers.api.processor.transaction;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.reactivex.Completable;

/**
 * A {@link Processor} used to set the Transaction and Request ID headers of the response.
 */
public class TransactionPostProcessor implements Processor {

    private final TransactionPostProcessorConfiguration configuration;

    public TransactionPostProcessor(TransactionPostProcessorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getId() {
        return "post-processor-transaction";
    }

    @Override
    public Completable execute(final MutableExecutionContext context) {
        return Completable.fromRunnable(() -> {
            setHeaderAccordingToBackendOverrideMode(
                context.request().headers(),
                context.response().headers(),
                this.configuration.transactionHeader,
                this.configuration.transactionHeaderHeaderOverrideMode
            );

            setHeaderAccordingToBackendOverrideMode(
                context.request().headers(),
                context.response().headers(),
                this.configuration.requestHeader,
                this.configuration.requestHeaderHeaderOverrideMode
            );
        });
    }

    private void setHeaderAccordingToBackendOverrideMode(
        final HttpHeaders requestHeaders,
        final HttpHeaders responseHeaders,
        final String headerName,
        final HeaderOverrideMode headerOverrideMode
    ) {
        String requestHeaderValue = requestHeaders.get(headerName);
        String backendHeaderValue = responseHeaders.get(headerName);

        if (headerOverrideMode == HeaderOverrideMode.OVERRIDE) {
            responseHeaders.set(headerName, requestHeaderValue);
        } else if (headerOverrideMode == HeaderOverrideMode.MERGE) {
            if (!requestHeaderValue.equals(backendHeaderValue)) {
                responseHeaders.add(headerName, requestHeaderValue);
            }
        } else if (headerOverrideMode == HeaderOverrideMode.KEEP) {
            responseHeaders.set(headerName, backendHeaderValue);
        }
    }
}

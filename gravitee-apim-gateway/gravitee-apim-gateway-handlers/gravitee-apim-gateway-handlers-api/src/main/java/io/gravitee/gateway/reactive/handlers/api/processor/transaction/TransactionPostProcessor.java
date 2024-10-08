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

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;

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
    public Completable execute(final HttpExecutionContextInternal context) {
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
        final TransactionHeaderOverrideMode headerOverrideMode
    ) {
        String requestHeaderValue = requestHeaders.get(headerName);
        String backendHeaderValue = responseHeaders.get(headerName);

        if (headerOverrideMode == TransactionHeaderOverrideMode.OVERRIDE) {
            if (requestHeaderValue != null) {
                responseHeaders.set(headerName, requestHeaderValue);
            }
        } else if (headerOverrideMode == TransactionHeaderOverrideMode.MERGE) {
            if (requestHeaderValue != null && !requestHeaderValue.equals(backendHeaderValue)) {
                responseHeaders.add(headerName, requestHeaderValue);
            }
        } else if (headerOverrideMode == TransactionHeaderOverrideMode.KEEP && backendHeaderValue != null) {
            responseHeaders.set(headerName, backendHeaderValue);
        }
    }
}

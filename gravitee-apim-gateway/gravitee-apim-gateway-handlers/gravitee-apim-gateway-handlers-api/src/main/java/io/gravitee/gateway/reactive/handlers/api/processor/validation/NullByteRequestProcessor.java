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
package io.gravitee.gateway.reactive.handlers.api.processor.validation;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import lombok.CustomLog;

/**
 * Rejects requests containing a NUL byte ({@code U+0000}) — literal or percent-encoded — in the
 * URI or any header. Added to the API chain only when {@code RequestValidation.rejectNullByte}
 * is enabled on the API.
 */
@CustomLog
public class NullByteRequestProcessor implements Processor {

    public static final String ID = "processor-null-byte-request-validation";
    public static final String NULL_BYTE_REJECTED_KEY = "REQUEST_NULL_BYTE_REJECTED";
    public static final String NULL_BYTE_REJECTED_MESSAGE = "The request contains invalid characters.";

    static final char NULL_CHAR = '\u0000';

    enum Source {
        URI("request URI"),
        HEADER("header");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private NullByteRequestProcessor() {}

    public static NullByteRequestProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.defer(() -> {
            final HttpBaseRequest request = ctx.request();

            final Detection detection = scan(request);
            if (detection == null) {
                return Completable.complete();
            }

            ctx
                .withLogger(log)
                .warn(
                    "Rejected request: null byte detected in {} '{}' (api={}, txn={})",
                    detection.source,
                    detection.name,
                    ctx.getAttribute(ContextAttributes.ATTR_API),
                    ctx.request().transactionId()
                );

            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).key(NULL_BYTE_REJECTED_KEY).message(NULL_BYTE_REJECTED_MESSAGE)
            );
        });
    }

    Detection scan(final HttpBaseRequest request) {
        // Path parameters are populated later in the chain (beforeApiExecution); the URI scan
        // below catches any null bytes that would surface through them.
        final String uri = request.uri();
        if (uri != null && (containsEncodedNullByte(uri) || containsNullByte(uri))) {
            return new Detection(Source.URI, "query");
        }
        return scanHeaders(request.headers());
    }

    private Detection scanHeaders(final HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers) {
            if (containsNullByte(entry.getKey()) || containsNullByte(entry.getValue()) || containsEncodedNullByte(entry.getValue())) {
                return new Detection(Source.HEADER, entry.getKey());
            }
        }
        return null;
    }

    static boolean containsNullByte(final String value) {
        return value != null && value.indexOf(NULL_CHAR) >= 0;
    }

    static boolean containsEncodedNullByte(final String value) {
        if (value == null || value.length() < 3) {
            return false;
        }
        int i = 0;
        while (true) {
            int percent = value.indexOf('%', i);
            if (percent < 0 || percent + 2 >= value.length()) {
                return false;
            }
            if (value.charAt(percent + 1) == '0' && value.charAt(percent + 2) == '0') {
                return true;
            }
            i = percent + 1;
        }
    }

    record Detection(Source source, String name) {}

    private static class Holder {

        private static final NullByteRequestProcessor INSTANCE = new NullByteRequestProcessor();
    }
}

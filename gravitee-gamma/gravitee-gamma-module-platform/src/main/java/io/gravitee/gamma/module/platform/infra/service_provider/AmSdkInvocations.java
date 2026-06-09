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
package io.gravitee.gamma.module.platform.infra.service_provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.am.sdk.management.invoker.ApiException;
import io.gravitee.gamma.module.platform.core.am.exception.AmUpstreamException;
import io.vertx.core.Future;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Confines the Vert.x Future dependency to infra/service_provider so core stays free of Vert.x
// types. Bounded timeout prevents a hung upstream call from tying up a JAX-RS worker thread.
// Messages here are viewer-facing, so they avoid internal "AM" terminology.
final class AmSdkInvocations {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String GENERIC_FAILURE = "The identity service request failed";
    private static final int MAX_PLAIN_TEXT_LENGTH = 500;

    private AmSdkInvocations() {}

    static <T> T await(Future<T> future) {
        return await(future, DEFAULT_TIMEOUT);
    }

    static <T> T await(Future<T> future, Duration timeout) {
        try {
            return future.toCompletionStage().toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmUpstreamException("Interrupted while contacting the identity service", null, e);
        } catch (TimeoutException e) {
            throw new AmUpstreamException("The identity service did not respond within " + timeout.toMillis() + "ms", 504, e);
        } catch (ExecutionException | CompletionException e) {
            throw translate(e.getCause());
        }
    }

    // Surfaces the readable field from an upstream error body without ever echoing raw JSON.
    static String describeBody(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception parseFailure) {
            // not JSON: show short plain text, but never a proxy HTML page or an oversized blob
            String trimmed = body.strip();
            if (trimmed.startsWith("<") || trimmed.length() > MAX_PLAIN_TEXT_LENGTH) {
                return fallback;
            }
            return trimmed;
        }
        for (String field : new String[] { "message", "error_description", "error" }) {
            JsonNode node = root.get(field);
            if (node != null && node.isTextual() && !node.asText().isBlank()) {
                return node.asText();
            }
        }
        return fallback;
    }

    private static RuntimeException translate(Throwable cause) {
        if (cause instanceof RuntimeException re) {
            if (cause.getClass().getPackageName().startsWith("io.gravitee.gamma.module.platform.core")) {
                return re;
            }
        }
        if (cause instanceof ApiException ae) {
            int status = ae.getCode();
            String fallback = ae.getMessage() == null ? GENERIC_FAILURE : ae.getMessage();
            return new AmUpstreamException(describeBody(ae.getResponseBody(), fallback), status, ae);
        }
        return new AmUpstreamException(cause == null ? GENERIC_FAILURE : cause.getMessage(), null, cause);
    }
}

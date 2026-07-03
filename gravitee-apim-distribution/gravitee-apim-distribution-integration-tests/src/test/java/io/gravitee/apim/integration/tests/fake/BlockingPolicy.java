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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;

/**
 * A V3-only legacy policy that blocks its {@code @OnRequest} method indefinitely.
 *
 * <p>Intentionally does NOT implement {@code io.gravitee.gateway.reactive.api.policy.Policy} so
 * that {@link io.gravitee.gateway.reactive.policy.HttpPolicyFactory} routes it through
 * {@link io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter} — the code path
 * protected by the PEN-88 worker-thread offloading and per-policy timeout fix.
 *
 * <p>Used by {@code BlockingPolicyTimeoutIntegrationTest} to verify that:
 * <ol>
 *   <li>The gateway returns an error response within {@code gateway.policy.groovy.timeoutMs}
 *       milliseconds rather than hanging for 30 seconds.</li>
 *   <li>The Vert.x event loop remains responsive while the blocking thread is parked.</li>
 * </ol>
 */
public class BlockingPolicy {

    public static final String POLICY_ID = "blocking-policy";

    /**
     * Blocks for up to 30 seconds without ever calling {@code chain.doNext()}.
     * The {@link io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter} timeout
     * is expected to abandon the reactive chain well before this deadline fires.
     */
    @OnRequest
    public void onRequest(final Request request, final Response response, final PolicyChain chain) {
        try {
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Intentionally never calls chain.doNext() or chain.failWith():
        // the PolicyAdapter .timeout() operator will fire first.
    }
}

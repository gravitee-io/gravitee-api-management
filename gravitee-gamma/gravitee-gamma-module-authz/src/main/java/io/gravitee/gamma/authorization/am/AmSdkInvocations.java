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
package io.gravitee.gamma.authorization.am;

import io.vertx.core.Future;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Bridges the AM SDK's Vert.x {@link Future} calls to a blocking result. The bounded timeout keeps
 * a hung upstream call from tying up the sync worker thread indefinitely. Mirrors the AIM module's
 * {@code AmSdkInvocations} pattern.
 */
final class AmSdkInvocations {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private AmSdkInvocations() {}

    static <T> T await(Future<T> future) {
        try {
            return future.toCompletionStage().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AmSyncException("Interrupted while contacting Access Management", e);
        } catch (TimeoutException e) {
            throw new AmSyncException("Access Management did not respond within " + DEFAULT_TIMEOUT.toMillis() + "ms", e);
        } catch (ExecutionException e) {
            throw new AmSyncException("Access Management request failed: " + e.getCause().getMessage(), e.getCause());
        }
    }
}

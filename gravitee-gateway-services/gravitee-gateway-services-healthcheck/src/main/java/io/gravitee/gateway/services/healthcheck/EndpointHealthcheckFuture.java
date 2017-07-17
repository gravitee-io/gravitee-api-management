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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.definition.model.Endpoint;

import java.util.concurrent.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class EndpointHealthcheckFuture<T> implements ScheduledFuture<T> {

    private final ScheduledFuture<T> delegateFuture;

    private final Endpoint endpoint;

    EndpointHealthcheckFuture(Endpoint endpoint, ScheduledFuture<T> delegateFuture) {
        this.endpoint = endpoint;
        this.delegateFuture = delegateFuture;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return delegateFuture.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return delegateFuture.compareTo(o);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegateFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegateFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegateFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegateFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegateFuture.get(timeout, unit);
    }
}

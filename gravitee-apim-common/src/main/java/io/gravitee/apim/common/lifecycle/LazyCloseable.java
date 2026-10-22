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
package io.gravitee.apim.common.lifecycle;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A resource built on first use and closed once, typically a Vert.x client built on an event loop by the first request
 * and closed by the thread that stops its owner.
 * <p>
 * Building and closing are serialised, so {@link #close()} never misses a resource being built, and a closed holder
 * stays closed: {@link #getOrBuild(Supplier)} then fails instead of building a resource nobody would close.
 */
public final class LazyCloseable<T> {

    private final Consumer<T> closer;

    // Volatile: read without the lock on the fast path, by every request once the resource is built.
    private volatile T resource;

    // Guarded by this.
    private boolean closed;

    public LazyCloseable(Consumer<T> closer) {
        this.closer = closer;
    }

    /**
     * @return the resource, built by {@code builder} on the first call
     * @throws IllegalStateException once {@link #close()} has been called
     */
    public T getOrBuild(Supplier<T> builder) {
        T current = resource;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Resource already closed");
            }
            if (resource == null) {
                resource = builder.get();
            }
            return resource;
        }
    }

    /** Closes the resource if it was built, and prevents any later build. Calling it again does nothing. */
    public synchronized void close() {
        closed = true;
        T current = resource;
        resource = null;
        if (current != null) {
            closer.accept(current);
        }
    }

    public synchronized boolean isClosed() {
        return closed;
    }
}

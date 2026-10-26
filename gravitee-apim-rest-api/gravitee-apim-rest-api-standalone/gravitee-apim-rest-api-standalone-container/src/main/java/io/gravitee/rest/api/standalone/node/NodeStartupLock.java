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
package io.gravitee.rest.api.standalone.node;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class NodeStartupLock {

    private final StartupLockRepository startupLockRepository;
    private final Duration timeout;
    private final AtomicReference<StartupLock> heldLock = new AtomicReference<>();

    public NodeStartupLock(StartupLockRepository startupLockRepository, Duration timeout) {
        this.startupLockRepository = startupLockRepository;
        this.timeout = timeout;
    }

    public void acquire() throws TechnicalException {
        heldLock.set(startupLockRepository.acquire(timeout));
    }

    public void release() {
        StartupLock lock = heldLock.getAndSet(null);
        if (lock != null) {
            lock.close();
        }
    }
}

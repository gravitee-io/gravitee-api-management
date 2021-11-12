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
package io.gravitee.rest.api.standalone.jetty;

import java.util.concurrent.ArrayBlockingQueue;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * JettyQueueThreadPool extends Jetty's default QueuedThreadPool,
 * Overriding thread creation to make them use the Gravitee context class loader.
 */
public class JettyQueuedThreadPool extends QueuedThreadPool {

    public JettyQueuedThreadPool(int poolMaxThreads, int poolMinThreads, int poolIdleTimeout, ArrayBlockingQueue<Runnable> queue) {
        super(poolMaxThreads, poolMinThreads, poolIdleTimeout, queue);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = super.newThread(runnable);
        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        return thread;
    }
}

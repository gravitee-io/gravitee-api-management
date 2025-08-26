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
package io.gravitee.gateway.reactive.core.connection;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ConnectionDrainManager {
    /**
     * Return the last requested drain timestamp or -1 if no drain has been requested.
     *
     * @return the last requested drain timestamp or -1 if no drain has been requested.
     */
    long drainRequestedAt();

    /**
     * Request connection draining.
     * This will update the last requested drain timestamp and invoke all registered listeners.
     */
    void requestDrain();

    /**
     * Register a listener that will be invoked when a drain is requested.
     *
     * @param listener the listener to invoke.
     *
     * @return the id of the listener which can be reused to unregister.
     */
    String registerListener(ConnectionDrainListener listener);

    /**
     * Unregister the corresponding listener.
     *
     * @param listenerId the listener to unregister.
     */
    void unregisterListener(String listenerId);
}

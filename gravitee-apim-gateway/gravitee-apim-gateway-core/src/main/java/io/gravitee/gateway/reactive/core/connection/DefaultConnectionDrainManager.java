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

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultConnectionDrainManager extends AbstractService<DefaultConnectionDrainManager> implements ConnectionDrainManager {

    private long drainRequestedAt = -1;
    private final Map<String, ConnectionDrainListener> listeners = new ConcurrentHashMap<>();

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        listeners.clear();
        drainRequestedAt = -1;
    }

    @Override
    public long drainRequestedAt() {
        return drainRequestedAt;
    }

    @Override
    public void requestDrain() {
        this.drainRequestedAt = System.currentTimeMillis();
        notifyListeners();
    }

    @Override
    public String registerListener(ConnectionDrainListener listener) {
        String listenerId = UUID.random().toString();
        listeners.put(listenerId, listener);
        return listenerId;
    }

    @Override
    public void unregisterListener(String listenerId) {
        listeners.remove(listenerId);
    }

    private void notifyListeners() {
        listeners.values().forEach(listener -> listener.accept(drainRequestedAt));
    }
}

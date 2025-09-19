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
package io.gravitee.gateway.handlers.accesspoint.manager;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.accesspoint.AccessPointEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DefaultAccessPointManager implements AccessPointManager {

    private final EventManager eventManager;
    private final Map<String, List<ReactableAccessPoint>> accessPoints = new ConcurrentHashMap<>();

    @Override
    public void register(ReactableAccessPoint reactableAccessPoint) {
        AtomicBoolean added = new AtomicBoolean(false);
        accessPoints.compute(reactableAccessPoint.getEnvironmentId(), (k, v) -> {
            if (v == null) {
                v = new CopyOnWriteArrayList<>();
            }
            if (!v.contains(reactableAccessPoint)) {
                added.set(v.add(reactableAccessPoint));
            }
            return v;
        });
        if (added.get()) {
            eventManager.publishEvent(AccessPointEvent.DEPLOY, reactableAccessPoint);
        } else {
            log.debug("Access point [{}] already registered, ignore it.", reactableAccessPoint);
        }
    }

    @Override
    public void unregister(ReactableAccessPoint reactableAccessPoint) {
        AtomicBoolean removed = new AtomicBoolean(false);
        accessPoints.computeIfPresent(reactableAccessPoint.getEnvironmentId(), (k, v) -> {
            removed.set(v.remove(reactableAccessPoint));
            if (v.isEmpty()) {
                return null;
            }
            return v;
        });
        if (removed.get()) {
            eventManager.publishEvent(AccessPointEvent.UNDEPLOY, reactableAccessPoint);
        } else {
            log.debug("Access point [{}] already unregistered, ignored it.", reactableAccessPoint);
        }
    }

    @Override
    public List<ReactableAccessPoint> getByEnvironmentId(final String environmentId) {
        if (environmentId != null) {
            return accessPoints.getOrDefault(environmentId, List.of());
        }
        return List.of();
    }
}

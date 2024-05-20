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

import io.gravitee.gateway.handlers.accesspoint.model.AccessPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccessPointManagerImpl implements AccessPointManager {

    private final Map<String, List<AccessPoint>> accessPoints = new ConcurrentHashMap<>();

    @Override
    public void register(AccessPoint accessPoint) {
        accessPoints.computeIfAbsent(accessPoint.getReferenceId(), id -> new ArrayList<>()).add(accessPoint);
    }

    @Override
    public void unregister(AccessPoint accessPoint) {
        List<AccessPoint> list = accessPoints.get(accessPoint.getReferenceId());
        if (list != null) {
            synchronized (list) {
                list.removeIf(ap -> Objects.equals(ap.getId(), accessPoint.getId()));
                if (list.isEmpty()) {
                    accessPoints.remove(accessPoint.getReferenceId());
                }
            }
        }
    }
}

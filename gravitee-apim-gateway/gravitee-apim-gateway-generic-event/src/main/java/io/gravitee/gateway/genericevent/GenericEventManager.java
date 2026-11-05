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
package io.gravitee.gateway.genericevent;

import io.gravitee.gateway.api.genericevent.GenericEvent;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import java.util.List;

/**
 * The sync side of the registry: the synchronizer stores events, then commits once per sync cycle.
 */
public interface GenericEventManager extends GenericEventRegistry {
    void upsert(GenericEvent event);

    void remove(String environmentId, String kind, String id);

    /**
     * Removes the expired events, notifying their subscribers, then calls onCommit on each subscriber whose kinds changed.
     *
     * @return the expired events removed
     */
    List<GenericEvent> commit();
}

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
package io.gravitee.gateway.reactive.core.v4.generic;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/** The observers of each kind, shared by the components that register them and the synchronizer that dispatches. */
@CustomLog
public class GenericEventObservers {

    private final Map<String, GenericEventObserver> observers = new ConcurrentHashMap<>();

    public void register(final GenericEventObserver observer) {
        observers.put(observer.kind(), observer);
        log.info("👂 Generic event observer registered for kind [{}]", observer.kind());
    }

    public Optional<GenericEventObserver> observerOf(final String kind) {
        return Optional.ofNullable(observers.get(kind));
    }
}

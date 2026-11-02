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

/** Receives the {@code GENERIC_EVENT}s of one kind. Must be registered before the first sync, or events are missed. */
public interface GenericEventObserver {
    String kind();

    /**
     * @return true once the event is handled ("done"): the synchronizer will not hand it out again. False, or an
     * exception, leaves it to be handed out again while the sync windows still cover it (about 30 s).
     */
    boolean onEvent(GenericEvent event);
}

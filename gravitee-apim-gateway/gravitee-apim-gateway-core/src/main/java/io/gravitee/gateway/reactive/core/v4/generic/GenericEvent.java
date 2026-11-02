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

/**
 * A {@code GENERIC_EVENT} as the synchronizer hands it out: routing metadata read from the event properties, and a
 * payload left raw for the observer of its kind to deserialize.
 *
 * @param id the event id
 * @param kind which observer receives it
 * @param action {@code apply} or {@code remove}
 * @param documentId the id of the document the event is about; one document, many events over time
 * @param payload raw JSON, never read by the synchronizer
 */
public record GenericEvent(String id, String kind, String action, String documentId, String payload) {
    public static final String ACTION_APPLY = "apply";
    public static final String ACTION_REMOVE = "remove";

    public boolean isRemove() {
        return ACTION_REMOVE.equals(action);
    }
}

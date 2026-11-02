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
package io.gravitee.gamma.rest.core.runtime_overlay.model;

import java.util.Map;

/** A runtime overlay as the control plane sees it, folded from its generic events. */
public record RuntimeOverlay(String id, String kind, String apiId, long startAt, long endAt, boolean stopped, Map<String, Object> payload) {
    public enum Status {
        ACTIVE,
        STOPPED,
        EXPIRED,
    }

    public Status statusAt(final long now) {
        if (stopped) {
            return Status.STOPPED;
        }
        return now >= endAt ? Status.EXPIRED : Status.ACTIVE;
    }

    public RuntimeOverlay stop() {
        return new RuntimeOverlay(id, kind, apiId, startAt, endAt, true, payload);
    }
}

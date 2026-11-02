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
package io.gravitee.gateway.reactive.core.v4.overlay;

import java.util.Map;

/**
 * A synchronized object that temporarily changes the behavior of a deployed API, without redeploying it.
 * The payload is specific to the kind and only read by the contributor of that kind.
 */
public record RuntimeOverlay(
    String id,
    String kind,
    String apiId,
    long startAt,
    long endAt,
    RuntimeOverlayStatus status,
    Map<String, Object> payload
) {
    public boolean hasEnded(final long now) {
        return now >= endAt;
    }
}

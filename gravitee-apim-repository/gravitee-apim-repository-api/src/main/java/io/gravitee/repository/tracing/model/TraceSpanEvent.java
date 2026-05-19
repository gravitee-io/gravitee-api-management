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
package io.gravitee.repository.tracing.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A timestamped event attached to a {@link TraceSpan} (e.g. {@code exception}, {@code gravitee.policy.pre},
 * {@code gravitee.policy.post}).
 *
 * @author GraviteeSource Team
 */
public record TraceSpanEvent(String name, Instant time, Map<String, String> attributes) {
    public TraceSpanEvent {
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }
    }
}

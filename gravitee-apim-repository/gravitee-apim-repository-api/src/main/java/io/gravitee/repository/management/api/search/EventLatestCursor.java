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
package io.gravitee.repository.management.api.search;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Position of the last event of a page, as the repository stores it: the date has no zone so that reading it back
 * cannot move it. {@code updatedAt} is {@code null} when that event has no update date.
 */
public record EventLatestCursor(LocalDateTime updatedAt, String id) {
    public EventLatestCursor {
        Objects.requireNonNull(id, "EventLatestCursor.id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("EventLatestCursor.id must not be blank");
        }
    }
}

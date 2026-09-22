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
package io.gravitee.rest.api.model.v4.log;

import java.util.List;

/**
 * @param total how many logs matched.
 * @param maxReachableTotal how many of them can actually be paged to, or {@code null} when unlimited. See
 *     {@code LogResponse} — the store can refuse pages beyond a window, and the paginator has to respect that
 *     while the count stays truthful.
 */
public record SearchLogsResponse<T>(long total, List<T> logs, Long maxReachableTotal) {
    public SearchLogsResponse(long total, List<T> logs) {
        this(total, logs, null);
    }
}

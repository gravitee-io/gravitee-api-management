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
package io.gravitee.repository.log.v4.model;

import io.gravitee.repository.analytics.query.response.Response;
import java.util.List;

/**
 * @param total how many documents matched, which is the honest count.
 * @param maxReachableTotal how many of them a caller can actually page to, or {@code null} when the store
 *     imposes no such limit. Elasticsearch refuses a from/size page past {@code index.max_result_window}, so
 *     on a busy API the two numbers differ and a paginator driven by {@code total} alone offers pages that
 *     come back as an error. Kept separate rather than clamping {@code total}, because the count is useful
 *     even where the rows are out of reach.
 */
public record LogResponse<T>(long total, List<T> data, Long maxReachableTotal) implements Response {
    public LogResponse(long total, List<T> data) {
        this(total, data, null);
    }
}

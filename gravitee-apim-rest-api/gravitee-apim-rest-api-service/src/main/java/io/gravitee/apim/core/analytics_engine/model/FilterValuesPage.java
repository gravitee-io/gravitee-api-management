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
package io.gravitee.apim.core.analytics_engine.model;

import java.util.List;
import java.util.Map;

/**
 * A page of filter values, with an opaque cursor for the next page.
 *
 * @param data               The filter values in this page
 * @param afterKey            Opaque cursor for requesting the next page. Null when there are no more pages.
 * @param totalFilteredCount  Total number of matching values across all pages. Null when the total is unknowable
 *                            (e.g. ES composite aggregations for KEYWORD filters).
 */
public record FilterValuesPage(List<FilterValue> data, Map<String, Object> afterKey, Long totalFilteredCount) {
    /** Convenience constructor for cursor-based pages where the total is unknown. */
    public FilterValuesPage(List<FilterValue> data, Map<String, Object> afterKey) {
        this(data, afterKey, null);
    }

    public boolean hasNextPage() {
        return afterKey != null && !afterKey.isEmpty();
    }
}

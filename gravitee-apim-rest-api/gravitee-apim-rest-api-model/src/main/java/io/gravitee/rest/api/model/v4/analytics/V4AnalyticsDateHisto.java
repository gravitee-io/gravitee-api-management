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
package io.gravitee.rest.api.model.v4.analytics;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a V4 analytics DATE_HISTO query: shared time buckets (timestamp) and per-field bucket counts (values).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class V4AnalyticsDateHisto {

    private List<Long> timestamp;
    private List<DateHistoValue> values;

    /** One series in the histogram: field name, counts per bucket, optional metadata. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateHistoValue {

        private String field;
        private List<Long> buckets;
        private Map<String, Object> metadata;
    }
}

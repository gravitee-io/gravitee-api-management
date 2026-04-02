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
package io.gravitee.repository.log.v4.model.analytics;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Aggregate result for a date-histogram query.
 *
 * <ul>
 *   <li>{@code timestamps} – ordered list of epoch-ms bucket-start values.</li>
 *   <li>{@code buckets} – one entry per distinct field value; counts are parallel to {@code timestamps}.</li>
 * </ul>
 */
@Data
@Builder
public class DateHistoAggregate {

    private List<Long> timestamps;
    private List<Bucket> buckets;

    /**
     * One time-series per distinct field value (e.g. per HTTP status code).
     */
    @Data
    @Builder
    public static class Bucket {

        /** The field value (e.g. "200", "500"). */
        private String field;

        /** Document count per time bucket, parallel to {@link DateHistoAggregate#timestamps}. */
        private List<Long> counts;

        /** Human-readable label, currently {@code {"name": "<value>"}}. */
        private Map<String, String> metadata;
    }
}

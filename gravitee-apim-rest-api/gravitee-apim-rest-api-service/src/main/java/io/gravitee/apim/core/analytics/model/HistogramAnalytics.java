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
package io.gravitee.apim.core.analytics.model;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record HistogramAnalytics(Timestamp timestamp, List<Bucket> buckets) {
    @Getter
    @RequiredArgsConstructor
    public abstract static class Bucket {

        private final String name;
        private final String field;
    }

    @Getter
    public static class CountBucket extends Bucket {

        private final Map<String, List<Long>> counts;

        public CountBucket(String name, String field, Map<String, List<Long>> counts) {
            super(name, field);
            this.counts = counts;
        }
    }

    @Getter
    public static class MetricBucket extends Bucket {

        private final List<Long> values;

        public MetricBucket(String name, String field, List<Long> values) {
            super(name, field);
            this.values = values;
        }
    }
}

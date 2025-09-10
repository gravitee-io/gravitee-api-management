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

public enum AggregationType {
    FIELD,
    AVG,
    MIN,
    MAX,
    /**
     * Returns a single value as the latest value for a given metric at the point in time.
     */
    VALUE,
    /**
     * Returns a single value as the difference between end and start values for a given metric in a given time range.
     */
    DELTA,
    /**
     * Returns multiple buckets to hold the delta aggregations in a given time range considering one bucket per interval specified in milliseconds.
     */
    TREND,
}

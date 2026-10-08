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
package io.gravitee.repository.analytics.engine.api.query;

/**
 * The family of analytics-engine searches a query goes through. Each one applies its own set of
 * filters, so the same condition can be applied on one path and skipped on another: an edge metric's
 * facets are filtered on the edge dimensions, its measures on the HTTP ones.
 */
public enum AnalyticsSearchPath {
    /** {@code searchHTTPMeasures}, {@code searchHTTPFacets}, {@code searchHTTPTimeSeries}. */
    HTTP,
    /** {@code searchEdgeFacets}. */
    EDGE,
    /** {@code searchMessageMeasures}, {@code searchMessageFacets}, {@code searchMessageTimeSeries}. */
    MESSAGE,
    /** {@code searchNativeApiFacets}, {@code searchNativeApiTimeSeries}. */
    NATIVE,
    /** {@code searchEventMetricsMeasures}, {@code searchEventMetricsFacets}, {@code searchEventMetricsTimeSeries}. */
    EVENT_METRICS,
    /** {@code searchAuthzMeasures}, {@code searchAuthzFacets}, {@code searchAuthzTimeSeries}. */
    AUTHZ,
    /** {@code searchAuthzTrafficMeasures}, {@code searchAuthzTrafficFacets}, {@code searchAuthzTrafficTimeSeries}. */
    AUTHZ_TRAFFIC,
}

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

/**
 * Discriminator for the unified analytics query endpoint.
 *
 * <ul>
 *   <li>COUNT     – total document hit count over a time range</li>
 *   <li>STATS     – min / max / avg / sum for a numeric field</li>
 *   <li>GROUP_BY  – top-N document counts grouped by a field (terms aggregation)</li>
 *   <li>DATE_HISTO – time-bucketed histogram (date_histogram aggregation)</li>
 * </ul>
 */
public enum AnalyticsType {
    COUNT,
    STATS,
    GROUP_BY,
    DATE_HISTO,
}

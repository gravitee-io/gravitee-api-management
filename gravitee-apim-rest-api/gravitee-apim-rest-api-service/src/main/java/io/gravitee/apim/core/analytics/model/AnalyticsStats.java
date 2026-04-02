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
 * Core domain model for statistical aggregates over a numeric metric field.
 *
 * @param count number of documents contributing to the aggregation
 * @param min   minimum value (null if no data)
 * @param max   maximum value (null if no data)
 * @param avg   average value (null if no data)
 * @param sum   sum of values (null if no data)
 */
public record AnalyticsStats(long count, Double min, Double max, Double avg, Double sum) {}

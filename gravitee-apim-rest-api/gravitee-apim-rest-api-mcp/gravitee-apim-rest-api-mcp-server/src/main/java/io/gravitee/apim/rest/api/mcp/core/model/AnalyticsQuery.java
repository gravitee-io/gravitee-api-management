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
package io.gravitee.apim.rest.api.mcp.core.model;

import java.util.List;

/**
 * A query in the analytics engine's own vocabulary, as a model writes it.
 *
 * @param value a string for EQ and CONTAINS, a number for LTE and GTE, a list of strings for IN
 */
public final class AnalyticsQuery {

    private AnalyticsQuery() {}

    public record Metric(String name, List<String> measures, List<Filter> filters, List<Sort> sorts) {}

    public record Filter(String name, String operator, Object value) {}

    public record Sort(String measure, String order) {}

    public record Range(Number from, Number to) {}
}

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
package io.gravitee.gamma.rest.core.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.CommonFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CommonFiltersTest {

    @Test
    void should_union_static_and_extensible_host_filter_names() {
        assertThat(CommonFilters.names()).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
        assertThat(CommonFilters.names()).contains(
            "API",
            "HTTP_STATUS",
            "API_TYPE",
            "HTTP_GATEWAY_RESPONSE_TIME",
            "MCP_PROXY_METHOD",
            "URI"
        );
    }

    @Test
    void should_not_expose_dropped_or_renamed_legacy_filter_names() {
        // URI replaces HTTP_PATH (empty on v4); HTTP_PATH_MAPPING is an analytics facet, not a filter;
        // the unified vocabulary uses the reconciled names (HTTP_GATEWAY_RESPONSE_TIME / MCP_PROXY_METHOD).
        assertThat(CommonFilters.names()).doesNotContain("HTTP_PATH", "HTTP_PATH_MAPPING", "RESPONSE_TIME", "MCP_METHOD");
    }
}

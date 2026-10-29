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
package io.gravitee.apim.core.analytics_engine.query_service;

import java.util.Map;
import java.util.Set;

/**
 * The additional metrics the built-in catalog already reads under a built-in name.
 *
 * @author GraviteeSource Team
 */
public interface BuiltInAdditionalMetricsQueryService {
    /**
     * Each additional metric key a built-in metric, filter or facet reads, type prefix included (e.g.
     * {@code keyword_mcp-proxy_tools/call}), with the built-in names that read it, on every path that resolves a
     * built-in name to a field: the analytics engine, the logs search and the filter values. A key the logs path reads
     * through a constant only maps to no name.
     */
    Map<String, Set<String>> namesByRawKey();
}

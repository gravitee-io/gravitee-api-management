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
package io.gravitee.gamma.rest.core.observability.catalog.port.service_provider;

import java.util.Map;
import java.util.Set;

/**
 * The additional metrics the built-in catalog reads under a built-in name.
 *
 * @author GraviteeSource Team
 */
public interface BuiltInAdditionalMetricsPort {
    /**
     * Each key a built-in metric, filter or facet reads, type prefix included, with the built-in names reading it.
     * A key the platform reads through a constant only maps to no name. A record or a discovered field on one of these
     * keys would duplicate the built-in entry.
     */
    Map<String, Set<String>> builtInNamesByRawKey();
}

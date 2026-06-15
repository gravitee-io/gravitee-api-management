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
package io.gravitee.gamma.rest.core.observability.filter.port.service_provider;

import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import java.util.List;
import java.util.Map;

/**
 * Module-facing extension point for the observability filter catalog. A module contributes in two
 * additive ways, both optional (default no-op):
 *
 * <ul>
 *   <li>{@link #filters()} — brand-new filters owned by the module (e.g. {@code LLM_MODEL}). Names
 *       must not collide with host-owned filters ({@code CommonFilters}); such collisions are
 *       rejected by the registry.</li>
 *   <li>{@link #enumValues()} — additional allowed values for a host-owned {@link ExtensibleFilters}
 *       (e.g. add {@code LLM}/{@code MCP} to {@code API_TYPE}). The key is the {@link ExtensibleFilters}
 *       enum, so a module can only add values to filters that are explicitly extensible — it can
 *       never modify a static filter's definition.</li>
 * </ul>
 *
 * <p>Implementations are pure data (no host internals). <b>Note:</b> runtime discovery of module
 * contributions across the isolated gamma plugin classloaders is NOT handled here — that registration
 * mechanism (plugin handler + {@code GammaModule} extension) is delivered separately. Today only
 * host-classpath contributors are wired.
 *
 * @author GraviteeSource Team
 */
public interface FilterContributor {
    /** New filters owned by the contributing module. */
    default List<FilterSpec> filters() {
        return List.of();
    }

    /** Additive values for host-owned {@link ExtensibleFilters} (e.g. {@code API_TYPE}). */
    default Map<ExtensibleFilters, List<FilterSpec.EnumValue>> enumValues() {
        return Map.of();
    }
}

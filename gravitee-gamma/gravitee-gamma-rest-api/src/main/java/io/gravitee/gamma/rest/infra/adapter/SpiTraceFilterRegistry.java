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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * {@link TraceFilterRegistry} adapter backed by {@link ServiceLoader}. Contributors are discovered
 * once at construction time — the SPI scan runs against the classpath the rest-api process was
 * booted with, which captures every gamma module's bundled JAR.
 *
 * <p>The aggregation logic is intentionally simple: each contributor's filters land in a
 * spec-by-{@code name} map in iteration order, so a later contributor overrides an earlier one
 * with the same {@code name}. Cross-module contributors ({@code moduleId() == null}) are applied
 * first, then the module-specific one — letting a module re-declare a common spec under the same
 * name to override its label / operators (no explicit override flag needed).
 *
 * @author GraviteeSource Team
 */
public class SpiTraceFilterRegistry implements TraceFilterRegistry {

    private final List<TraceFilterContributor> contributors;

    public SpiTraceFilterRegistry() {
        this(ServiceLoader.load(TraceFilterContributor.class).stream().map(ServiceLoader.Provider::get).toList());
    }

    /** Test seam — lets a domain test inject a fixed contributor list without touching the classpath. */
    public SpiTraceFilterRegistry(List<TraceFilterContributor> contributors) {
        this.contributors = List.copyOf(contributors);
    }

    @Override
    public List<TraceFilterSpec> getFiltersForModule(String moduleId) {
        Map<String, TraceFilterSpec> bySpecName = new LinkedHashMap<>();
        // Cross-module first so module-specific contributors can override by name.
        for (TraceFilterContributor c : contributors) {
            if (c.moduleId() == null) {
                for (TraceFilterSpec spec : c.getFilters()) {
                    bySpecName.put(spec.name(), spec);
                }
            }
        }
        if (moduleId != null) {
            for (TraceFilterContributor c : contributors) {
                if (Objects.equals(moduleId, c.moduleId())) {
                    for (TraceFilterSpec spec : c.getFilters()) {
                        bySpecName.put(spec.name(), spec);
                    }
                }
            }
        }
        return new ArrayList<>(bySpecName.values());
    }
}

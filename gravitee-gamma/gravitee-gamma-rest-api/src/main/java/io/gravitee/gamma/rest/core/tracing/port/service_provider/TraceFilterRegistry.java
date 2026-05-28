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
package io.gravitee.gamma.rest.core.tracing.port.service_provider;

import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import java.util.List;

/**
 * Core-side port that returns the aggregated filter specs for a given module. The infra adapter
 * is responsible for discovering {@link TraceFilterContributor} implementations (typically via
 * {@link java.util.ServiceLoader}); the use case stays unaware of how contributors are wired.
 *
 * @author GraviteeSource Team
 */
public interface TraceFilterRegistry {
    /**
     * Returns the union of:
     * <ul>
     *   <li>every cross-module contributor's filters (those whose
     *       {@link TraceFilterContributor#moduleId()} is {@code null}), and</li>
     *   <li>contributions from the contributor whose {@code moduleId()} matches the {@code moduleId}
     *       argument — when non-null.</li>
     * </ul>
     * De-duplication is by {@link TraceFilterSpec#name()} with last-write-wins semantics, so a
     * module contributor can override a cross-module entry by re-declaring it under the same name.
     *
     * @param moduleId The {@code gravitee.module} value the caller is interested in, or {@code null}
     *                 to introspect every cross-module filter without module-specific additions.
     */
    List<TraceFilterSpec> getFiltersForModule(String moduleId);
}

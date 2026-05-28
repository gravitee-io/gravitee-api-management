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
 * Java SPI for gamma modules to contribute their own trace filter specifications. The
 * {@code TraceFilterRegistry} discovers implementations via {@link java.util.ServiceLoader} at boot,
 * so a module ships its contributions by:
 * <ol>
 *   <li>Implementing this interface in its own Maven artifact.</li>
 *   <li>Declaring the impl class in
 *       {@code META-INF/services/io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor}.</li>
 * </ol>
 * No Spring wiring needed on the contributor side — implementations are stateless and resolved
 * once at JVM start. Contributions are intrinsic to the module's code; there is no runtime
 * registration / hot-reload mechanism.
 *
 * <p>The gamma-rest-api itself ships {@code CommonTraceFilterContributor} for cross-module
 * (Tier-1+2) filters. Per-module contributors (e.g. AIM's LLM / MCP filters) live in their own
 * module's codebase.
 *
 * @author GraviteeSource Team
 */
public interface TraceFilterContributor {
    /**
     * The {@code gravitee.module} value this contributor applies to (e.g. {@code "aim"},
     * {@code "apim"}). Returning {@code null} marks the contribution as cross-module — included in
     * every response regardless of which {@code module} the caller asked for.
     */
    String moduleId();

    /**
     * The filter specs this contributor adds. Returned list should be effectively immutable —
     * implementations typically expose a {@code List.of(...)} constant.
     */
    List<TraceFilterSpec> getFilters();
}

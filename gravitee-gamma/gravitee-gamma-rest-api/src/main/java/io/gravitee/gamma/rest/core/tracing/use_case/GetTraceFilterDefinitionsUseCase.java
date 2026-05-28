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
package io.gravitee.gamma.rest.core.tracing.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterRegistry;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Returns the set of filter specs available for a given module — the UI calls this once per page
 * load to build its filter chip palette without hardcoded knowledge of what the backend supports.
 *
 * <p>{@code moduleId} is intentionally optional: a {@code null} argument yields only the
 * cross-module ({@code moduleId() == null}) contributors' filters, which is useful for an
 * "introspection" UI that lists every always-available filter.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetTraceFilterDefinitionsUseCase {

    private final TraceFilterRegistry filterRegistry;

    public record Input(String moduleId) {}

    public record Output(List<TraceFilterSpec> filters) {}

    public Output execute(Input input) {
        return new Output(filterRegistry.getFiltersForModule(input.moduleId()));
    }
}

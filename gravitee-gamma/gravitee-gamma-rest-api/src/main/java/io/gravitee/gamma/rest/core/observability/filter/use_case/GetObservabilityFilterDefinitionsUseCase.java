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
package io.gravitee.gamma.rest.core.observability.filter.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Returns the self-describing catalog of observability filters, narrowed on the two independent
 * discovery axes ({@link Signal} and {@link ApiType}). The UI calls this once per panel to build its
 * filter chip palette dynamically and to know which filters it may apply to which signal — without
 * hardcoded value→label maps (each {@code ENUM} spec carries its labelled values).
 *
 * <p>Both axes are optional: a {@code null}/empty set on an axis is unconstrained, so an empty input
 * on both yields the full catalog. The registry keeps a spec when its own non-empty axis intersects
 * the requested one (see {@link FilterRegistry#getFilters(Set, Set)}).
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityFilterDefinitionsUseCase {

    private final FilterRegistry filterRegistry;

    public record Input(Set<Signal> signals, Set<ApiType> apiTypes) {}

    public record Output(List<FilterSpec> filters) {}

    public Output execute(Input input) {
        return new Output(filterRegistry.getFilters(input.signals(), input.apiTypes()));
    }
}

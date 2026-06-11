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
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * Bulk hydration of stored filter ids into display labels, grouped per filter. The UI uses this to
 * render readable chips from URL-encoded id-based filter state ({@code API}/{@code APPLICATION}/
 * {@code PLAN}/{@code API_PRODUCT}) without keeping a label map client-side. Non-resolvable filters
 * yield an empty label map (no error), so a mixed request degrades gracefully.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ResolveObservabilityFilterLabelsUseCase {

    private final ObservabilityFilterDataPort filterDataPort;

    public record Entry(String filterName, List<String> ids) {}

    public record ResolvedEntry(String filterName, Map<String, String> labels) {}

    public record Input(List<Entry> entries) {}

    public record Output(List<ResolvedEntry> entries) {}

    public Output execute(Input input) {
        List<Entry> entries = input.entries() == null ? List.of() : input.entries();
        var requests = entries
            .stream()
            .map(e -> new ObservabilityFilterDataPort.ResolveRequest(e.filterName(), e.ids()))
            .toList();
        var resolved = filterDataPort
            .resolveLabels(requests)
            .stream()
            .map(r -> new ResolvedEntry(r.filterName(), r.labels()))
            .toList();
        return new Output(resolved);
    }
}

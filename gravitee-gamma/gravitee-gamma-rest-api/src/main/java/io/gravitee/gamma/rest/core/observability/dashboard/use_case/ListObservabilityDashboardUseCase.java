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
package io.gravitee.gamma.rest.core.observability.dashboard.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Lists every dashboard saved in the caller's environment, paginated.
 *
 * <p>{@link DashboardRepository#findByEnvironmentId} has no native pagination (the OBS-14 SPI
 * returns the full, ordered list) — pagination is applied here by slicing the full result, matching
 * the "fetch, then slice" precedent used by {@code TracingPortAdapter} for the same reason. Fine for
 * a per-environment dashboard count, which is expected to stay small.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class ListObservabilityDashboardUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;
    private static final int MAX_PER_PAGE = 100;

    private final DashboardRepository dashboardRepository;

    public record Input(String environmentId, Integer page, Integer perPage) {}

    public record Output(List<Dashboard> dashboards, long totalCount, int page, int perPage) {}

    public Output execute(Input input) {
        int page = resolvePage(input);
        int perPage = resolvePerPage(input);

        List<Dashboard> all = dashboardRepository.findByEnvironmentId(input.environmentId());
        int fromIndex = Math.min((page - 1) * perPage, all.size());
        int toIndex = Math.min(fromIndex + perPage, all.size());

        return new Output(all.subList(fromIndex, toIndex), all.size(), page, perPage);
    }

    private static int resolvePage(Input input) {
        return (input.page() != null && input.page() > 0) ? input.page() : DEFAULT_PAGE;
    }

    private static int resolvePerPage(Input input) {
        return (input.perPage() != null && input.perPage() > 0) ? Math.min(input.perPage(), MAX_PER_PAGE) : DEFAULT_PER_PAGE;
    }
}

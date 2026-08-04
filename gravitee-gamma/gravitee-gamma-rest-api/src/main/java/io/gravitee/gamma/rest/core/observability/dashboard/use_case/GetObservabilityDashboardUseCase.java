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
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardNotFoundException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import lombok.AllArgsConstructor;

/**
 * Fetches a single dashboard, scoped to the caller's environment. A dashboard id from another
 * environment throws {@link DashboardNotFoundException} — 404, not 403 — via the same
 * environment-scoped repository lookup used by the list use case, so existence cannot be probed
 * across environments.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class GetObservabilityDashboardUseCase {

    private final DashboardRepository dashboardRepository;

    public record Input(String environmentId, String dashboardId) {}

    public record Output(Dashboard dashboard) {}

    public Output execute(Input input) {
        Dashboard dashboard = dashboardRepository
            .findByIdAndEnvironmentId(input.dashboardId(), input.environmentId())
            .orElseThrow(() -> new DashboardNotFoundException(input.dashboardId()));
        return new Output(dashboard);
    }
}

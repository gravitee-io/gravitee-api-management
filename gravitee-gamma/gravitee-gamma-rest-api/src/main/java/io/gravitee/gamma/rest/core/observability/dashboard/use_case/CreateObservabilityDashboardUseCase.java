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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import java.time.Instant;
import lombok.AllArgsConstructor;

/**
 * Creates a dashboard from author-supplied {@link DashboardContent} (OBS-16). The id is
 * client-supplied (AGENTS.md §9 — resource-creation ids come from the request); every server-owned
 * field is derived here: {@code version} starts at 1, {@code createdBy} is the authenticated caller,
 * {@code createdAt}/{@code updatedAt} are stamped now. Version is maintained but not enforced — the
 * optimistic-locking guard is OBS-17.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class CreateObservabilityDashboardUseCase {

    private static final int INITIAL_VERSION = 1;

    private final DashboardRepository dashboardRepository;

    public record Input(String environmentId, String createdBy, String dashboardId, DashboardContent content) {}

    public record Output(Dashboard dashboard) {}

    public Output execute(Input input) {
        input.content().validate();
        Instant now = TimeProvider.instantNow();
        Dashboard dashboard = new Dashboard(
            input.dashboardId(),
            input.environmentId(),
            input.content().title(),
            input.content().description(),
            input.content().filters(),
            input.content().timeRange(),
            input.content().widgets(),
            INITIAL_VERSION,
            input.createdBy(),
            now,
            now
        );
        return new Output(dashboardRepository.create(dashboard));
    }
}

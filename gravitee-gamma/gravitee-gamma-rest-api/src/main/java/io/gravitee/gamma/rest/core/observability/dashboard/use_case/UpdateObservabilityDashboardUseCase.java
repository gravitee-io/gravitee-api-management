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
import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardNotFoundException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardContent;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import lombok.AllArgsConstructor;

/**
 * Replaces a dashboard's author-editable content (OBS-16). Starts from the existing aggregate so the
 * server-owned fields survive the write: {@code createdAt} and {@code createdBy} are carried over,
 * {@code updatedAt} is stamped now and {@code version} is incremented. A stale version is not
 * rejected here — behaviour stays last-write-wins until OBS-17 lands the guard; a partial check
 * would advertise a guarantee that still has a TOCTOU hole on the Mongo path.
 *
 * <p>A dashboard id from another environment throws {@link DashboardNotFoundException} — 404, not
 * 403 — via the same environment-scoped lookup as the read path, so cross-environment existence
 * cannot be probed.
 *
 * @author GraviteeSource Team
 */
@UseCase
@AllArgsConstructor
public class UpdateObservabilityDashboardUseCase {

    private final DashboardRepository dashboardRepository;

    public record Input(String environmentId, String dashboardId, DashboardContent content) {}

    public record Output(Dashboard dashboard) {}

    public Output execute(Input input) {
        input.content().validate();
        Dashboard existing = dashboardRepository
            .findByIdAndEnvironmentId(input.dashboardId(), input.environmentId())
            .orElseThrow(() -> new DashboardNotFoundException(input.dashboardId()));
        Dashboard updated = new Dashboard(
            existing.id(),
            existing.environmentId(),
            input.content().title(),
            input.content().description(),
            input.content().filters(),
            input.content().timeRange(),
            input.content().widgets(),
            nextVersion(existing),
            existing.createdBy(),
            existing.createdAt(),
            TimeProvider.instantNow()
        );
        return new Output(dashboardRepository.update(updated));
    }

    /**
     * Rows written before OBS-16 may carry a {@code null} version (the OBS-14 repository persists it
     * verbatim, never initialises it) — treat them as unversioned and (re)start the counter.
     */
    private static int nextVersion(Dashboard existing) {
        return existing.version() == null ? 1 : existing.version() + 1;
    }
}

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
package io.gravitee.gamma.rest.core.observability.dashboard.port.repository;

import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import java.util.List;
import java.util.Optional;

/**
 * Port onto Gamma's own dashboard storage — first legitimate use of {@code port/repository/} in this
 * module (see AGENTS.md §2): unlike {@code TracingPort}, which fronts an external OTel-backed SPI,
 * dashboards are data this module persists itself (via the {@code GammaDashboardRepository} SPI
 * shipped by OBS-14).
 *
 * <p>Deliberately does not declare {@code findAll()}: it would be unscoped on an environment-scoped
 * entity, so keeping it off this port makes it unreachable above the adapter.
 *
 * @author GraviteeSource Team
 */
public interface DashboardRepository {
    /**
     * Ordered by creation date, then id — see {@code GammaDashboardRepository#findByEnvironmentId}.
     */
    List<Dashboard> findByEnvironmentId(String environmentId);

    /**
     * Environment-scoped lookup: a dashboard id from another environment resolves to empty here
     * rather than as a caller-side check the single-dashboard use case would otherwise have to
     * remember to perform.
     */
    Optional<Dashboard> findByIdAndEnvironmentId(String id, String environmentId);

    Dashboard create(Dashboard dashboard);

    Dashboard update(Dashboard dashboard);

    /**
     * Takes a bare id because environment scoping happens before deletion: the delete use case
     * resolves the dashboard through {@link #findByIdAndEnvironmentId} first, so a cross-environment
     * id 404s without ever reaching this method.
     */
    void delete(String id);
}

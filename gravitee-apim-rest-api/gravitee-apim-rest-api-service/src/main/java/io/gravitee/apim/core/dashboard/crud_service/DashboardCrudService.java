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
package io.gravitee.apim.core.dashboard.crud_service;

import io.gravitee.apim.core.dashboard.model.Dashboard;
import java.util.List;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DashboardCrudService {
    Dashboard create(Dashboard dashboard);

    /**
     * A dashboard of another environment resolves to empty, like a missing one, so callers answer 404 and never
     * disclose that the id exists elsewhere.
     */
    Optional<Dashboard> findByIdAndEnvironmentId(String id, String environmentId);

    List<Dashboard> findByEnvironmentId(String environmentId);

    /** Unscoped: resolve the dashboard with {@link #findByIdAndEnvironmentId} first. */
    Dashboard update(Dashboard dashboard);

    /** Unscoped: resolve the dashboard with {@link #findByIdAndEnvironmentId} first. */
    void delete(String id);
}

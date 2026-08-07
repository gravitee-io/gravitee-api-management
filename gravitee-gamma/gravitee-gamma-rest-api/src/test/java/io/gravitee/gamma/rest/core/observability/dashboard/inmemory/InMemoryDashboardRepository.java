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
package io.gravitee.gamma.rest.core.observability.dashboard.inmemory;

import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.core.observability.dashboard.port.repository.DashboardRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link DashboardRepository} for domain tests. Filters seeded dashboards on environment
 * scope just like the real adapter would scope the Mongo/JDBC query, so use-case tests exercise the
 * environment-isolation contract end-to-end rather than mocking it away.
 */
public class InMemoryDashboardRepository implements DashboardRepository {

    private final List<Dashboard> dashboards = new ArrayList<>();

    public void givenDashboard(Dashboard dashboard) {
        dashboards.add(dashboard);
    }

    public void reset() {
        dashboards.clear();
    }

    @Override
    public List<Dashboard> findByEnvironmentId(String environmentId) {
        return dashboards
            .stream()
            .filter(d -> Objects.equals(d.environmentId(), environmentId))
            .toList();
    }

    @Override
    public Optional<Dashboard> findByIdAndEnvironmentId(String id, String environmentId) {
        return dashboards
            .stream()
            .filter(d -> Objects.equals(d.id(), id) && Objects.equals(d.environmentId(), environmentId))
            .findFirst();
    }

    @Override
    public Dashboard create(Dashboard dashboard) {
        dashboards.add(dashboard);
        return dashboard;
    }

    @Override
    public Dashboard update(Dashboard dashboard) {
        // Replaces in place: the real backends order by creation date, so an update must not
        // reshuffle the list the way remove-then-add would.
        for (int i = 0; i < dashboards.size(); i++) {
            if (Objects.equals(dashboards.get(i).id(), dashboard.id())) {
                dashboards.set(i, dashboard);
                return dashboard;
            }
        }
        dashboards.add(dashboard);
        return dashboard;
    }

    @Override
    public void delete(String id) {
        dashboards.removeIf(d -> Objects.equals(d.id(), id));
    }
}

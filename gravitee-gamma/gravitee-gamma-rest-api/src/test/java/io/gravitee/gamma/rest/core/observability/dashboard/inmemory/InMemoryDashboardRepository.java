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

    private Dashboard pendingConcurrentSave;

    private String pendingConcurrentDelete;

    public void givenDashboard(Dashboard dashboard) {
        dashboards.add(dashboard);
    }

    public void reset() {
        dashboards.clear();
        pendingConcurrentSave = null;
        pendingConcurrentDelete = null;
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
    public Optional<Dashboard> updateIfPresent(Dashboard dashboard) {
        runPendingConcurrentSave();
        return replaceInPlace(dashboard);
    }

    /**
     * Replaces in place: the real backends order by creation date, so an update must not reshuffle the list the way
     * remove-then-add would. Absent means absent — no upsert, matching both backends, so a test cannot accidentally
     * assert on a dashboard the real store would have refused to resurrect.
     */
    private Optional<Dashboard> replaceInPlace(Dashboard dashboard) {
        for (int i = 0; i < dashboards.size(); i++) {
            if (Objects.equals(dashboards.get(i).id(), dashboard.id())) {
                dashboards.set(i, dashboard);
                return Optional.of(dashboard);
            }
        }
        return Optional.empty();
    }

    /**
     * Compares against the <em>stored</em> version, exactly as the real conditional query does, so the guard is
     * genuinely exercised rather than mocked away.
     */
    @Override
    public Optional<Dashboard> updateIfVersionMatches(Dashboard dashboard, int expectedVersion) {
        runPendingConcurrentSave();
        return findByIdAndEnvironmentId(dashboard.id(), dashboard.environmentId())
            .filter(stored -> Objects.equals(stored.version(), expectedVersion))
            .flatMap(stored -> replaceInPlace(dashboard));
    }

    /**
     * Runs {@code save} inside the next conditional write, i.e. after the caller has read the dashboard and decided
     * its version still matched. That interleaving is the whole point of the storage-level guard, and it cannot be
     * reproduced by seeding the store up front — the caller's own read would see it and refuse earlier.
     */
    public void givenAConcurrentSaveBeforeTheNextVersionedWrite(Dashboard winner) {
        this.pendingConcurrentSave = winner;
    }

    /** Same idea for a delete landing after the caller's read — the case an overwrite must still lose to. */
    public void givenADeleteBeforeTheNextWrite(String id) {
        this.pendingConcurrentDelete = id;
    }

    private void runPendingConcurrentSave() {
        if (pendingConcurrentSave != null) {
            Dashboard winner = pendingConcurrentSave;
            pendingConcurrentSave = null;
            replaceInPlace(winner);
        }
        if (pendingConcurrentDelete != null) {
            String doomed = pendingConcurrentDelete;
            pendingConcurrentDelete = null;
            delete(doomed);
        }
    }

    @Override
    public void delete(String id) {
        dashboards.removeIf(d -> Objects.equals(d.id(), id));
    }
}

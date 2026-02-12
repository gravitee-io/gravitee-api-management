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
package inmemory;

import io.gravitee.apim.core.dashboard.crud_service.DashboardCrudService;
import io.gravitee.apim.core.dashboard.model.Dashboard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class DashboardCrudServiceInMemory implements DashboardCrudService, InMemoryAlternative<Dashboard> {

    final ArrayList<Dashboard> storage = new ArrayList<>();

    @Override
    public Dashboard create(Dashboard dashboard) {
        storage.add(dashboard);
        return dashboard;
    }

    @Override
    public Optional<Dashboard> findById(String dashboardId) {
        return storage
            .stream()
            .filter(d -> dashboardId.equals(d.getId()))
            .findFirst();
    }

    @Override
    public Dashboard update(Dashboard dashboard) {
        var index = findIndex(storage, d -> d.getId().equals(dashboard.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), dashboard);
            return dashboard;
        }
        throw new IllegalStateException("Dashboard not found");
    }

    @Override
    public void delete(String dashboardId) {
        storage.removeIf(d -> dashboardId.equals(d.getId()));
    }

    @Override
    public void initWith(List<Dashboard> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Dashboard> storage() {
        return Collections.unmodifiableList(storage);
    }
}

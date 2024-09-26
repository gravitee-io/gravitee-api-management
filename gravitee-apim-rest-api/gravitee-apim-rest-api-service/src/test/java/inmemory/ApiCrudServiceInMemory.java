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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class ApiCrudServiceInMemory implements ApiCrudService, InMemoryAlternative<Api> {

    final ArrayList<Api> storage = new ArrayList<>();

    @Override
    public Collection<Api> find(Collection<String> ids) {
        return storage.stream().filter(api -> ids.contains(api.getId())).toList();
    }

    @Override
    public boolean existsById(String id) {
        return storage.stream().anyMatch(api -> id.equals(api.getId()));
    }

    @Override
    public Api create(Api api) {
        storage.add(api);
        return api;
    }

    @Override
    public Api update(Api api) {
        OptionalInt index = this.findIndex(this.storage, plan -> plan.getId().equals(api.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), api);
            return api;
        }

        throw new IllegalStateException("API not found");
    }

    @Override
    public void delete(String id) {
        storage.removeIf(api -> id.equals(api.getId()));
    }

    @Override
    public void initWith(List<Api> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Api> storage() {
        return Collections.unmodifiableList(storage);
    }
}

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

import io.gravitee.apim.core.integration.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.integration.model.AsyncJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class AsyncJobCrudServiceInMemory implements AsyncJobCrudService, InMemoryAlternative<AsyncJob> {

    final ArrayList<AsyncJob> storage = new ArrayList<>();

    @Override
    public AsyncJob create(AsyncJob integration) {
        storage.add(integration);
        return integration;
    }

    @Override
    public Optional<AsyncJob> findById(String id) {
        return storage.stream().filter(item -> item.getId().equals(id)).findFirst();
    }

    @Override
    public AsyncJob update(AsyncJob integration) {
        OptionalInt index = this.findIndex(this.storage, i -> i.getId().equals(integration.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), integration);
            return integration;
        }

        throw new IllegalStateException("AsyncJob not found");
    }

    @Override
    public void delete(String id) {
        OptionalInt index = this.findIndex(this.storage, i -> i.getId().equals(id));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void initWith(List<AsyncJob> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<AsyncJob> storage() {
        return Collections.unmodifiableList(storage);
    }
}

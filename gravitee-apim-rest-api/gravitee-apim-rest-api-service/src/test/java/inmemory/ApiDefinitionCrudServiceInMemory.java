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

import io.gravitee.apim.core.api.crud_service.ApiDefinitionCrudService;
import io.gravitee.apim.core.api.model.Api;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The targeted write a store does: it starts from the API <em>as stored</em> and moves two fields of it, so a change
 * made to another field since the caller read it is still there afterwards. A double that wrote the caller's whole
 * copy back would hide the very defect this port exists to close.
 *
 * <p>The Guardian index the real write carries alongside the definition is not kept here: the double stores the
 * definition, and {@link ApiQueryServiceInMemory} reads the Guardians out of it — the same answer, from one place.
 */
public class ApiDefinitionCrudServiceInMemory implements ApiDefinitionCrudService, InMemoryAlternative<Api> {

    private final List<Api> storage;

    public ApiDefinitionCrudServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public ApiDefinitionCrudServiceInMemory(ApiCrudServiceInMemory apiCrudServiceInMemory) {
        this.storage = apiCrudServiceInMemory.storage;
    }

    /** Runs once, right before the next definition write: what a concurrent writer does in that window. */
    public Runnable beforeNextReplace = () -> {};

    @Override
    public boolean replaceDefinition(Api api, ZonedDateTime expectedUpdatedAt) {
        var concurrently = beforeNextReplace;
        beforeNextReplace = () -> {};
        concurrently.run();
        var index = findIndex(storage, stored -> Objects.equals(stored.getId(), api.getId()));
        if (index.isEmpty()) {
            return false;
        }
        var stored = storage.get(index.getAsInt());
        if (!Objects.equals(stored.getUpdatedAt(), expectedUpdatedAt)) {
            return false;
        }
        storage.set(
            index.getAsInt(),
            stored.toBuilder().apiDefinitionValue(api.getApiDefinitionValue()).updatedAt(api.getUpdatedAt()).build()
        );
        return true;
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
        return List.copyOf(storage);
    }
}

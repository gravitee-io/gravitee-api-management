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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.model.Sortable;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ApiQueryServiceInMemory implements ApiQueryService, InMemoryAlternative<Api> {

    private Storage<Api> storage = new Storage<>();

    /**
     * WARNING: this implementation doesn't actually filter the API present in the storage. Instead, it will return all applications from storage.
     */
    @Override
    public Stream<Api> search(ApiSearchCriteria apiCriteria, Sortable sortable, ApiFieldFilter apiFieldFilter) {
        return this.storage().data().stream();
    }

    @Override
    public Optional<Api> findByEnvironmentIdAndCrossId(String environmentId, String crossId) {
        return storage
            .data()
            .stream()
            .filter(api -> api.getEnvironmentId().equals(environmentId) && api.getCrossId().equals(crossId))
            .findFirst();
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public Storage<Api> storage() {
        return storage;
    }

    @Override
    public void syncStorageWith(InMemoryAlternative<Api> other) {
        storage = other.storage();
    }
}

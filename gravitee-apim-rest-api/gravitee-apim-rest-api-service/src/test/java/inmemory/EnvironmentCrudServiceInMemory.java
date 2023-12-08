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

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnvironmentCrudServiceInMemory implements EnvironmentCrudService, InMemoryAlternative<Environment> {

    private Storage<Environment> storage = new Storage<>();

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public Storage<Environment> storage() {
        return storage;
    }

    @Override
    public Environment get(String environmentId) {
        return storage
            .data()
            .stream()
            .filter(env -> environmentId.equals(env.getId()))
            .findFirst()
            .orElseThrow(() -> new EnvironmentNotFoundException(environmentId));
    }

    @Override
    public void syncStorageWith(InMemoryAlternative<Environment> other) {
        storage = other.storage();
    }
}

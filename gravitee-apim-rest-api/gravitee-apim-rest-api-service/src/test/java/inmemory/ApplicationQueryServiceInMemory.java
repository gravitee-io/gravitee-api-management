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

import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationQueryServiceInMemory implements ApplicationQueryService, InMemoryAlternative<BaseApplicationEntity> {

    private final List<BaseApplicationEntity> storage;

    public ApplicationQueryServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public ApplicationQueryServiceInMemory(ApplicationCrudServiceInMemory applicationCrudServiceInMemory) {
        storage = applicationCrudServiceInMemory.storage;
    }

    @Override
    public void initWith(List<BaseApplicationEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<BaseApplicationEntity> storage() {
        return Collections.unmodifiableList(storage);
    }

    @Override
    public Set<BaseApplicationEntity> findByEnvironment(String environmentId) {
        return storage.stream().filter(application -> application.getEnvironmentId().equals(environmentId)).collect(Collectors.toSet());
    }
}

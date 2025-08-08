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

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationCrudServiceInMemory implements ApplicationCrudService, InMemoryAlternative<BaseApplicationEntity> {

    final List<BaseApplicationEntity> storage = new ArrayList<>();

    @Override
    public BaseApplicationEntity findById(ExecutionContext executionContext, String applicationId) {
        return storage
            .stream()
            .filter(application -> applicationId.equals(application.getId()))
            .findFirst()
            .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    @Override
    public BaseApplicationEntity findById(String applicationId, String environmentId) {
        return storage
            .stream()
            .filter(application -> applicationId.equals(application.getId()))
            .findFirst()
            .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    @Override
    public List<BaseApplicationEntity> findByIds(List<String> appIds, String environmentId) {
        return storage
            .stream()
            .filter(app -> appIds.contains(app.getId()))
            .filter(app -> app.getEnvironmentId().equals(environmentId))
            .collect(Collectors.toList());
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
}

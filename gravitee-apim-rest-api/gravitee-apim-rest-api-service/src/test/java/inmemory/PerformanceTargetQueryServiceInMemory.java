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

import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceTargetQueryServiceInMemory implements PerformanceTargetQueryService, InMemoryAlternative<PerformanceTarget> {

    private final List<PerformanceTarget> storage;

    public PerformanceTargetQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public PerformanceTargetQueryServiceInMemory(PerformanceTargetCrudServiceInMemory crudService) {
        storage = crudService.storage;
    }

    @Override
    public List<PerformanceTarget> findAll() {
        return List.copyOf(storage);
    }

    @Override
    public List<PerformanceTarget> findByReference(String environmentId, String reference) {
        return storage
            .stream()
            .filter(target -> target.environmentId().equals(environmentId) && target.subject().reference().equals(reference))
            .toList();
    }

    @Override
    public void initWith(List<PerformanceTarget> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<PerformanceTarget> storage() {
        return Collections.unmodifiableList(storage);
    }
}

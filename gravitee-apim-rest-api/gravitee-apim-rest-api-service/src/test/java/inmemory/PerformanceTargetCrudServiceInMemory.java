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

import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetCrudService;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PerformanceTargetCrudServiceInMemory implements PerformanceTargetCrudService, InMemoryAlternative<PerformanceTarget> {

    final ArrayList<PerformanceTarget> storage = new ArrayList<>();

    @Override
    public PerformanceTarget create(PerformanceTarget target) {
        storage.add(target);
        return target;
    }

    @Override
    public Optional<PerformanceTarget> findById(String id) {
        return storage
            .stream()
            .filter(target -> target.id().equals(id))
            .findFirst();
    }

    @Override
    public PerformanceTarget update(PerformanceTarget target) {
        var index = findIndex(storage, stored -> stored.id().equals(target.id()));
        if (index.isEmpty()) {
            throw new IllegalStateException("Performance target not found");
        }
        storage.set(index.getAsInt(), target);
        return target;
    }

    @Override
    public void delete(String id) {
        storage.removeIf(target -> target.id().equals(id));
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) {
        var deleted = storage
            .stream()
            .filter(target -> target.environmentId().equals(environmentId) && target.subject().reference().equals(reference))
            .map(PerformanceTarget::id)
            .toList();
        storage.removeIf(target -> deleted.contains(target.id()));
        return deleted;
    }

    @Override
    public List<String> removeApiId(String apiId) {
        var affected = new ArrayList<String>();
        for (int i = 0; i < storage.size(); i++) {
            var target = storage.get(i);
            if (target.subject().apiIds().contains(apiId)) {
                affected.add(target.id());
                var remaining = target
                    .subject()
                    .apiIds()
                    .stream()
                    .filter(id -> !id.equals(apiId))
                    .toList();
                storage.set(i, target.toBuilder().subject(new PerformanceTarget.Subject(remaining, target.subject().reference())).build());
            }
        }
        return affected;
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

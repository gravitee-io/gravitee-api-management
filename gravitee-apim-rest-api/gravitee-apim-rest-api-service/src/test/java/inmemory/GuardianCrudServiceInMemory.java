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

import io.gravitee.apim.core.guardian.crud_service.GuardianCrudService;
import io.gravitee.apim.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.apim.core.guardian.model.Guardian;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import lombok.NoArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class GuardianCrudServiceInMemory implements GuardianCrudService, InMemoryAlternative<Guardian> {

    final ArrayList<Guardian> storage = new ArrayList<>();

    public GuardianCrudServiceInMemory(List<Guardian> items) {
        initWith(items);
    }

    @Override
    public Guardian create(Guardian guardian) {
        storage.add(guardian);
        return guardian;
    }

    @Override
    public Guardian getByEnvironmentId(String environmentId, String guardianId) {
        return storage
            .stream()
            .filter(guardian -> guardianId.equals(guardian.getId()) && environmentId.equals(guardian.getEnvironmentId()))
            .findFirst()
            .orElseThrow(() -> new GuardianNotFoundException(guardianId));
    }

    @Override
    public Guardian update(Guardian guardian) {
        OptionalInt index = this.findIndex(this.storage, existing -> existing.getId().equals(guardian.getId()));
        if (index.isPresent()) {
            storage.set(index.getAsInt(), guardian);
            return guardian;
        }
        throw new IllegalStateException("Guardian not found");
    }

    @Override
    public void delete(String guardianId) {
        storage.removeIf(guardian -> guardianId.equals(guardian.getId()));
    }

    @Override
    public void initWith(List<Guardian> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Guardian> storage() {
        return List.copyOf(storage);
    }
}

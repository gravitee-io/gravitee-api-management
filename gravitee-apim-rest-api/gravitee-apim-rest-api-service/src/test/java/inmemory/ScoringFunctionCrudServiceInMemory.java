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

import io.gravitee.apim.core.scoring.crud_service.ScoringFunctionCrudService;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ScoringFunctionCrudServiceInMemory implements ScoringFunctionCrudService, InMemoryAlternative<ScoringFunction> {

    final ArrayList<ScoringFunction> storage = new ArrayList<>();

    @Override
    public ScoringFunction create(ScoringFunction function) {
        deleteByReferenceAndName(function.referenceId(), function.referenceType(), function.name());
        storage.add(function);
        return function;
    }

    @Override
    public Optional<ScoringFunction> findById(String id) {
        return storage
            .stream()
            .filter(i -> i.id().equals(id))
            .findFirst();
    }

    @Override
    public void delete(String id) {
        OptionalInt index = this.findIndex(this.storage, i -> i.id().equals(id));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void deleteByReference(String referenceId, ScoringFunction.ReferenceType referenceType) {
        OptionalInt index = this.findIndex(
            this.storage,
            i -> i.referenceId().equals(referenceId) && i.referenceType().equals(referenceType)
        );
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void deleteByReferenceAndName(String referenceId, ScoringFunction.ReferenceType referenceType, String name) {
        storage.removeIf(
            fct -> name.equals(fct.name()) && referenceId.equals(fct.referenceId()) && referenceType.equals(fct.referenceType())
        );
    }

    @Override
    public void initWith(List<ScoringFunction> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ScoringFunction> storage() {
        return Collections.unmodifiableList(storage);
    }
}

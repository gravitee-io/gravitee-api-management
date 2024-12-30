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

import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.apim.core.scoring.query_service.ScoringFunctionQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoringFunctionQueryServiceInMemory implements ScoringFunctionQueryService, InMemoryAlternative<ScoringFunction> {

    private final List<ScoringFunction> storage;

    public ScoringFunctionQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ScoringFunctionQueryServiceInMemory(ScoringFunctionCrudServiceInMemory integrationCrudServiceInMemory) {
        storage = integrationCrudServiceInMemory.storage;
    }

    @Override
    public List<ScoringFunction> findByReference(String referenceId, ScoringFunction.ReferenceType referenceType) {
        return storage
            .stream()
            .filter(report -> report.referenceId().equals(referenceId) && report.referenceType().equals(referenceType))
            .toList();
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

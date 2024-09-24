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

import io.gravitee.apim.core.scoring.crud_service.ScoringRulesetCrudService;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class ScoringRulesetCrudServiceInMemory implements ScoringRulesetCrudService, InMemoryAlternative<ScoringRuleset> {

    final ArrayList<ScoringRuleset> storage = new ArrayList<>();

    @Override
    public ScoringRuleset create(ScoringRuleset ruleset) {
        storage.add(ruleset);
        return ruleset;
    }

    @Override
    public Optional<ScoringRuleset> findById(String id) {
        return storage.stream().filter(i -> i.id().equals(id)).findFirst();
    }

    @Override
    public void delete(String id) {
        OptionalInt index = this.findIndex(this.storage, i -> i.id().equals(id));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void deleteByReference(String referenceId, ScoringRuleset.ReferenceType referenceType) {
        OptionalInt index =
            this.findIndex(this.storage, i -> i.referenceId().equals(referenceId) && i.referenceType().equals(referenceType));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void initWith(List<ScoringRuleset> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ScoringRuleset> storage() {
        return Collections.unmodifiableList(storage);
    }
}

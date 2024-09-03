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

import io.gravitee.apim.core.scoring.crud_service.ScoringReportCrudService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class ScoringReportCrudServiceInMemory implements ScoringReportCrudService, InMemoryAlternative<ScoringReport> {

    final ArrayList<ScoringReport> storage = new ArrayList<>();

    @Override
    public ScoringReport create(ScoringReport integration) {
        storage.add(integration);
        return integration;
    }

    @Override
    public void deleteByApi(String apiId) {
        OptionalInt index = this.findIndex(this.storage, i -> i.apiId().equals(apiId));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
    }

    @Override
    public void initWith(List<ScoringReport> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ScoringReport> storage() {
        return Collections.unmodifiableList(storage);
    }
}

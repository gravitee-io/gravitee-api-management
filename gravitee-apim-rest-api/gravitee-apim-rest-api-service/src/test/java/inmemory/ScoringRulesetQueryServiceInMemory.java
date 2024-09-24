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

import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.model.EnvironmentOverview;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import io.gravitee.apim.core.scoring.query_service.ScoringRulesetQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ScoringRulesetQueryServiceInMemory implements ScoringRulesetQueryService, InMemoryAlternative<ScoringRuleset> {

    private final List<ScoringRuleset> storage;

    public ScoringRulesetQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public ScoringRulesetQueryServiceInMemory(ScoringRulesetCrudServiceInMemory integrationCrudServiceInMemory) {
        storage = integrationCrudServiceInMemory.storage;
    }

    @Override
    public List<ScoringRuleset> findByReference(String referenceId, ScoringRuleset.ReferenceType referenceType) {
        return storage
            .stream()
            .filter(report -> report.referenceId().equals(referenceId) && report.referenceType().equals(referenceType))
            .toList();
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

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
package io.gravitee.apim.infra.query_service.scoring;

import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.core.scoring.query_service.ScoringRulesetQueryService;
import io.gravitee.apim.infra.adapter.ScoringRulesetAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringRulesetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringRulesetQueryServiceImpl extends AbstractService implements ScoringRulesetQueryService {

    private final ScoringRulesetRepository scoringRulesetRepository;

    public ScoringRulesetQueryServiceImpl(@Lazy ScoringRulesetRepository scoringRulesetRepository) {
        this.scoringRulesetRepository = scoringRulesetRepository;
    }

    @Override
    public List<ScoringRuleset> findByReference(String referenceId, ScoringRuleset.ReferenceType referenceType) {
        try {
            return scoringRulesetRepository
                .findAllByReferenceId(referenceId, referenceType.name())
                .stream()
                .map(ScoringRulesetAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Scoring ruleset [{}:{}]", referenceType, referenceId, e);
            throw new TechnicalManagementException(
                "An error occurred while finding Scoring ruleset [%s:%s] ".formatted(referenceType, referenceId),
                e
            );
        }
    }
}

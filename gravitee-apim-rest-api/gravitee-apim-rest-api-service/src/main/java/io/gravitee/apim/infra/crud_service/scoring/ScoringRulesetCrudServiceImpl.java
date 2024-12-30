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
package io.gravitee.apim.infra.crud_service.scoring;

import io.gravitee.apim.core.scoring.crud_service.ScoringRulesetCrudService;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.infra.adapter.ScoringReportAdapter;
import io.gravitee.apim.infra.adapter.ScoringRulesetAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringRulesetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScoringRulesetCrudServiceImpl extends AbstractService implements ScoringRulesetCrudService {

    private final ScoringRulesetRepository scoringRulesetRepository;

    public ScoringRulesetCrudServiceImpl(@Lazy ScoringRulesetRepository scoringRulesetRepository) {
        this.scoringRulesetRepository = scoringRulesetRepository;
    }

    @Override
    public ScoringRuleset create(ScoringRuleset ruleset) {
        try {
            var created = scoringRulesetRepository.create(ScoringRulesetAdapter.INSTANCE.toRepository(ruleset));
            return ScoringRulesetAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Scoring Ruleset: " + ruleset.id(), e);
        }
    }

    @Override
    public Optional<ScoringRuleset> findById(String id) {
        try {
            return scoringRulesetRepository.findById(id).map(ScoringRulesetAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when searching for Scoring Ruleset: " + id, e);
        }
    }

    @Override
    public ScoringRuleset update(ScoringRuleset ruleset) {
        try {
            var updated = scoringRulesetRepository.update(ScoringRulesetAdapter.INSTANCE.toRepository(ruleset));
            return ScoringRulesetAdapter.INSTANCE.toEntity(updated);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when updating Scoring Ruleset: " + ruleset.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            scoringRulesetRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Scoring Ruleset: %s".formatted(id), e);
        }
    }

    @Override
    public void deleteByReference(String referenceId, ScoringRuleset.ReferenceType referenceType) {
        try {
            scoringRulesetRepository.deleteByReferenceId(referenceId, referenceType.name());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when deleting Scoring Ruleset for [%s:%s]".formatted(referenceType, referenceId),
                e
            );
        }
    }
}

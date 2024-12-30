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

import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.apim.core.scoring.query_service.ScoringFunctionQueryService;
import io.gravitee.apim.infra.adapter.ScoringFunctionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringFunctionQueryServiceImpl extends AbstractService implements ScoringFunctionQueryService {

    private final ScoringFunctionRepository scoringFunctionRepository;

    public ScoringFunctionQueryServiceImpl(@Lazy ScoringFunctionRepository scoringFunctionRepository) {
        this.scoringFunctionRepository = scoringFunctionRepository;
    }

    @Override
    public List<ScoringFunction> findByReference(String referenceId, ScoringFunction.ReferenceType referenceType) {
        try {
            return scoringFunctionRepository
                .findAllByReferenceId(referenceId, referenceType.name())
                .stream()
                .map(ScoringFunctionAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Scoring function [{}:{}]", referenceType, referenceId, e);
            throw new TechnicalManagementException(
                "An error occurred while finding Scoring function [%s:%s] ".formatted(referenceType, referenceId),
                e
            );
        }
    }
}

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

import io.gravitee.apim.core.scoring.crud_service.ScoringFunctionCrudService;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.apim.infra.adapter.ScoringFunctionAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScoringFunctionCrudServiceImpl extends AbstractService implements ScoringFunctionCrudService {

    private final ScoringFunctionRepository scoringFunctionRepository;

    public ScoringFunctionCrudServiceImpl(@Lazy ScoringFunctionRepository scoringFunctionRepository) {
        this.scoringFunctionRepository = scoringFunctionRepository;
    }

    @Override
    public ScoringFunction create(ScoringFunction function) {
        try {
            var created = scoringFunctionRepository.create(ScoringFunctionAdapter.INSTANCE.toRepository(function));
            return ScoringFunctionAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Scoring Function: " + function.id(), e);
        }
    }

    @Override
    public Optional<ScoringFunction> findById(String id) {
        try {
            return scoringFunctionRepository.findById(id).map(ScoringFunctionAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when searching for Scoring Function: " + id, e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            scoringFunctionRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Scoring Function: %s".formatted(id), e);
        }
    }

    @Override
    public void deleteByReference(String referenceId, ScoringFunction.ReferenceType referenceType) {
        try {
            scoringFunctionRepository.deleteByReferenceId(referenceId, referenceType.name());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when deleting Scoring Function for [%s:%s]".formatted(referenceType, referenceId),
                e
            );
        }
    }
}

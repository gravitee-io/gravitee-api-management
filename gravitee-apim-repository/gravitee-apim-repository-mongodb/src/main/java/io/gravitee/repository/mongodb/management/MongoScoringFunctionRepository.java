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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.ScoringFunction;
import io.gravitee.repository.mongodb.management.internal.integration.IntegrationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import io.gravitee.repository.mongodb.management.internal.model.ScoringFunctionMongo;
import io.gravitee.repository.mongodb.management.internal.score.ScoringFunctionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoScoringFunctionRepository implements ScoringFunctionRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScoringFunctionMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public Optional<ScoringFunction> findById(String s) throws TechnicalException {
        logger.debug("Find function by id [{}]", s);
        var result = internalRepository.findById(s).map(this::map);
        logger.debug("Find function by id [{}] - Done", s);
        return result;
    }

    @Override
    public ScoringFunction create(ScoringFunction function) throws TechnicalException {
        logger.debug("Create function [{}]", function.getId());
        var created = map(internalRepository.insert(map(function)));
        logger.debug("Create function [{}] - Done", created.getId());
        return created;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete function [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete function [{}] - Done", id);
    }

    @Override
    public List<ScoringFunction> findAllByReferenceId(String referenceId, String referenceType) {
        logger.debug("Search by reference [{}={}]", referenceType, referenceId);

        var result = internalRepository.findByReferenceIdAndReferenceType(referenceId, referenceType).stream().map(mapper::map).toList();

        logger.debug("Search by reference [{}={}] - Done", referenceType, referenceId);
        return result;
    }

    @Override
    public List<String> deleteByReferenceId(String referenceId, String referenceType) throws TechnicalException {
        logger.debug("Delete by reference: [{}={}]", referenceType, referenceId);
        try {
            List<String> all = internalRepository
                .deleteByReferenceIdAAndReferenceType(referenceId, referenceType)
                .stream()
                .map(ScoringFunctionMongo::getId)
                .toList();
            logger.debug("Delete by reference: [{}={}] - Done", referenceType, referenceId);
            return all;
        } catch (Exception ex) {
            logger.error("Failed to delete function by Reference: [{}={}]", referenceType, referenceId, ex);
            throw new TechnicalException("Failed to delete function by Reference");
        }
    }

    private ScoringFunction map(ScoringFunctionMongo source) {
        return mapper.map(source);
    }

    private ScoringFunctionMongo map(ScoringFunction source) {
        return mapper.map(source);
    }
}

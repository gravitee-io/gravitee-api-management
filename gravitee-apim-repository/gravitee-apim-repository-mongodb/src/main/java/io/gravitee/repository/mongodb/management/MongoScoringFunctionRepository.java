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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringFunctionRepository;
import io.gravitee.repository.management.model.ScoringFunction;
import io.gravitee.repository.mongodb.management.internal.model.ScoringFunctionMongo;
import io.gravitee.repository.mongodb.management.internal.score.ScoringFunctionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CustomLog
@Component
@RequiredArgsConstructor
public class MongoScoringFunctionRepository implements ScoringFunctionRepository {

    private final ScoringFunctionMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public Optional<ScoringFunction> findById(String s) throws TechnicalException {
        log.debug("Find function by id [{}]", s);
        var result = internalRepository.findById(s).map(this::map);
        log.debug("Find function by id [{}] - Done", s);
        return result;
    }

    @Override
    public ScoringFunction create(ScoringFunction function) throws TechnicalException {
        log.debug("Create function [{}]", function.getId());
        var created = map(internalRepository.insert(map(function)));
        log.debug("Create function [{}] - Done", created.getId());
        return created;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete function [{}]", id);
        internalRepository.deleteById(id);
        log.debug("Delete function [{}] - Done", id);
    }

    @Override
    public List<ScoringFunction> findAllByReferenceId(String referenceId, String referenceType) {
        log.debug("Search by reference [{}={}]", referenceType, referenceId);

        var result = internalRepository.findByReferenceIdAndReferenceType(referenceId, referenceType).stream().map(mapper::map).toList();

        log.debug("Search by reference [{}={}] - Done", referenceType, referenceId);
        return result;
    }

    @Override
    public List<String> deleteByReferenceId(String referenceId, String referenceType) throws TechnicalException {
        log.debug("Delete by reference: [{}={}]", referenceType, referenceId);
        try {
            List<String> all = internalRepository
                .deleteByReferenceIdAAndReferenceType(referenceId, referenceType)
                .stream()
                .map(ScoringFunctionMongo::getId)
                .toList();
            log.debug("Delete by reference: [{}={}] - Done", referenceType, referenceId);
            return all;
        } catch (Exception ex) {
            log.error("Failed to delete function by Reference: [{}={}]", referenceType, referenceId, ex);
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

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
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.repository.management.model.ScoringReport;
import io.gravitee.repository.mongodb.management.internal.model.ScoringReportMongo;
import io.gravitee.repository.mongodb.management.internal.score.ScoringReportMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MongoScoringReportRepository implements ScoringReportRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ScoringReportMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public ScoringReport create(ScoringReport report) throws TechnicalException {
        logger.debug("Create scoring report [{}]", report.getId());
        var created = map(internalRepository.insert(map(report)));
        logger.debug("Create scoring report [{}] - Done", created.getId());
        return created;
    }

    @Override
    public Optional<ScoringReport> findLatestFor(String apiId) {
        logger.debug("Find scoring report by api [{}]", apiId);
        var result = internalRepository.findLatestByApiId(apiId);
        logger.debug("Find scoring report by api [{}] - DONE", apiId);
        return result.map(mapper::map);
    }

    @Override
    public void deleteByApi(String apiId) {
        logger.debug("Delete scoring reports of API [{}]", apiId);
        internalRepository.deleteByApiId(apiId);
        logger.debug("Delete scoring reports of API [{}] - Done", apiId);
    }

    private ScoringReport map(ScoringReportMongo source) {
        return mapper.map(source);
    }

    private ScoringReportMongo map(ScoringReport source) {
        return mapper.map(source);
    }
}

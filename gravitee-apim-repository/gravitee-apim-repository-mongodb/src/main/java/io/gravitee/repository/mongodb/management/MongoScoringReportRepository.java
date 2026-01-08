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
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ScoringEnvironmentApi;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.management.model.ScoringReport;
import io.gravitee.repository.mongodb.management.internal.model.ScoringReportMongo;
import io.gravitee.repository.mongodb.management.internal.score.ScoringReportMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoScoringReportRepository implements ScoringReportRepository {

    @Autowired
    private ScoringReportMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public ScoringReport create(ScoringReport report) throws TechnicalException {
        log.debug("Create scoring report [{}]", report.getId());
        var created = map(internalRepository.insert(map(report)));
        log.debug("Create scoring report [{}] - Done", created.getId());
        return created;
    }

    @Override
    public Optional<ScoringReport> findLatestFor(String apiId) {
        log.debug("Find scoring report by api [{}]", apiId);
        var result = internalRepository.findLatestByApiId(apiId);
        log.debug("Find scoring report by api [{}] - DONE", apiId);
        return result.map(mapper::map);
    }

    @Override
    public Stream<ScoringReport> findLatestReports(Collection<String> apiIds) {
        log.debug("Find latest scoring reports of {}", apiIds);
        var result = internalRepository.findLatestReports(apiIds);
        log.debug("Find latest scoring reports of {} - DONE", apiIds);
        return result.stream().map(mapper::map);
    }

    @Override
    public Page<ScoringEnvironmentApi> findEnvironmentLatestReports(String environmentId, Pageable pageable) {
        log.debug("Find all latest scoring reports of environment {}", environmentId);
        var result = internalRepository.findEnvironmentLatestReports(environmentId, pageable);
        log.debug("Find all latest scoring reports of environment {} - DONE", environmentId);
        return result;
    }

    @Override
    public void deleteByApi(String apiId) {
        log.debug("Delete scoring reports of API [{}]", apiId);
        internalRepository.deleteByApiId(apiId);
        log.debug("Delete scoring reports of API [{}] - Done", apiId);
    }

    @Override
    public ScoringEnvironmentSummary getScoringEnvironmentSummary(String environmentId) throws TechnicalException {
        log.debug("Get Environment Scoring Summary of {}", environmentId);
        var result = internalRepository.getScoringEnvironmentSummary(environmentId);
        log.debug("Get Environment Scoring Summary of {} - DONE", environmentId);
        return result;
    }

    private ScoringReport map(ScoringReportMongo source) {
        return mapper.map(source);
    }

    private ScoringReportMongo map(ScoringReport source) {
        return mapper.map(source);
    }
}

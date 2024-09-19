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

import io.gravitee.apim.core.scoring.model.EnvironmentOverview;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import io.gravitee.apim.infra.adapter.ScoringReportAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringReportQueryServiceImpl implements ScoringReportQueryService {

    private final ScoringReportRepository scoringReportRepository;

    public ScoringReportQueryServiceImpl(@Lazy ScoringReportRepository scoringReportRepository) {
        this.scoringReportRepository = scoringReportRepository;
    }

    @Override
    public Optional<ScoringReport> findLatestByApiId(String apiId) {
        try {
            return scoringReportRepository.findLatestFor(apiId).map(ScoringReportAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Scoring Report by API id", e);
            throw new TechnicalManagementException("An error occurred while finding Scoring Report of API: " + apiId, e);
        }
    }

    @Override
    public Stream<ScoringReport> findLatestReportsByApiId(Collection<String> apiIds) {
        try {
            return scoringReportRepository.findLatestReports(apiIds).map(ScoringReportAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Scoring Reports by API id", e);
            throw new TechnicalManagementException("An error occurred while finding Scoring Report of API: " + apiIds, e);
        }
    }

    @Override
    public EnvironmentOverview getEnvironmentScoringSummary(String environmentId) {
        try {
            return ScoringReportAdapter.INSTANCE.toEntity(scoringReportRepository.getScoringEnvironmentSummary(environmentId));
        } catch (TechnicalException e) {
            log.error("An error occurred while getting Scoring Environment Summary of {}", environmentId, e);
            throw new TechnicalManagementException("An error occurred while getting Scoring Environment Summary of " + environmentId, e);
        }
    }
}

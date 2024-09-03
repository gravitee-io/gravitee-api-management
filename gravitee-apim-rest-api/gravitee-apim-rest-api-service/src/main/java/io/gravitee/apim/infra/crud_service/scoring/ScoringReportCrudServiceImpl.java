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

import io.gravitee.apim.core.scoring.crud_service.ScoringReportCrudService;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import io.gravitee.apim.infra.adapter.ScoringReportAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScoringReportCrudServiceImpl extends AbstractService implements ScoringReportCrudService {

    private final ScoringReportRepository scoringReportRepository;

    public ScoringReportCrudServiceImpl(@Lazy ScoringReportRepository scoringReportRepository) {
        this.scoringReportRepository = scoringReportRepository;
    }

    @Override
    public ScoringReport create(ScoringReport scoringReport) {
        try {
            var created = scoringReportRepository.create(ScoringReportAdapter.INSTANCE.toRepository(scoringReport));
            return ScoringReportAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Scoring Report for API: " + scoringReport.apiId(), e);
        }
    }

    @Override
    public void deleteByApi(String apiId) {
        try {
            scoringReportRepository.deleteByApi(apiId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Scoring Report for API: " + apiId, e);
        }
    }
}

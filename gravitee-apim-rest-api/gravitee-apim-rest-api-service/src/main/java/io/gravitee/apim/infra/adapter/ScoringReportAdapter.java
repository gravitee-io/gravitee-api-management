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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.model.EnvironmentOverview;
import io.gravitee.apim.core.scoring.model.ScoringReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ScoringReportAdapter {
    ScoringReportAdapter INSTANCE = Mappers.getMapper(ScoringReportAdapter.class);

    ScoringReport toEntity(io.gravitee.repository.management.model.ScoringReport source);
    EnvironmentOverview toEntity(io.gravitee.repository.management.model.ScoringEnvironmentSummary source);

    default EnvironmentApiScoringReport toEntity(io.gravitee.repository.management.model.ScoringEnvironmentApi source) {
        return new EnvironmentApiScoringReport(
            toEnvironmentApiScoringReportApi(source),
            source.getReportId() != null ? toEnvironmentApiScoringReportSummary(source) : null
        );
    }

    @Mapping(target = "apiId", source = "apiId")
    @Mapping(target = "name", source = "apiName")
    @Mapping(target = "updatedAt", source = "apiUpdatedAt")
    EnvironmentApiScoringReport.Api toEnvironmentApiScoringReportApi(io.gravitee.repository.management.model.ScoringEnvironmentApi source);

    @Mapping(target = "id", source = "reportId")
    @Mapping(target = "createdAt", source = "reportCreatedAt")
    EnvironmentApiScoringReport.Summary toEnvironmentApiScoringReportSummary(
        io.gravitee.repository.management.model.ScoringEnvironmentApi source
    );

    io.gravitee.repository.management.model.ScoringReport toRepository(ScoringReport source);
}

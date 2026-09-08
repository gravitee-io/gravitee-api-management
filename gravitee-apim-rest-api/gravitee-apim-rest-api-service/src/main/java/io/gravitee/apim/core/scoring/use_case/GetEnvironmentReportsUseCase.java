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
package io.gravitee.apim.core.scoring.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.scoring.model.EnvironmentApiScoringReport;
import io.gravitee.apim.core.scoring.query_service.ScoringReportQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetEnvironmentReportsUseCase {

    private final ScoringReportQueryService scoringReportQueryService;

    public Output execute(Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        var result = scoringReportQueryService.findEnvironmentLatestReports(input.environmentId(), input.apiTypes(), pageable);

        return new Output(result);
    }

    /**
     * @param apiTypes restricts the result to these API types; {@code null} or empty returns every type.
     */
    public record Input(String environmentId, Collection<ApiType> apiTypes, Optional<Pageable> pageable) {
        public Input(String environmentId) {
            this(environmentId, null, Optional.empty());
        }

        public Input(String environmentId, Pageable pageable) {
            this(environmentId, null, Optional.ofNullable(pageable));
        }

        public Input(String environmentId, Collection<ApiType> apiTypes, Pageable pageable) {
            this(environmentId, apiTypes, Optional.ofNullable(pageable));
        }
    }

    public record Output(Page<EnvironmentApiScoringReport> reports) {}
}

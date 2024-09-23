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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.ScoringEnvironmentApi;
import io.gravitee.repository.management.model.ScoringEnvironmentSummary;
import io.gravitee.repository.management.model.ScoringReport;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface ScoringReportRepository {
    ScoringReport create(ScoringReport report) throws TechnicalException;
    Optional<ScoringReport> findLatestFor(String apiId) throws TechnicalException;
    Stream<ScoringReport> findLatestReports(Collection<String> apiIds) throws TechnicalException;
    Page<ScoringEnvironmentApi> findEnvironmentLatestReports(String environmentId, Pageable pageable) throws TechnicalException;
    void deleteByApi(String api) throws TechnicalException;

    ScoringEnvironmentSummary getScoringEnvironmentSummary(String environmentId) throws TechnicalException;
}

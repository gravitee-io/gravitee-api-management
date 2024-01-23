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
package io.gravitee.apim.core.audit.use_case;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.domain_service.SearchAuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SearchApiAuditUseCase {

    private final SearchAuditDomainService searchAuditDomainService;

    public SearchApiAuditUseCase(SearchAuditDomainService searchAuditDomainService) {
        this.searchAuditDomainService = searchAuditDomainService;
    }

    public Output execute(Input input) {
        var query = input.query;
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));

        MetadataPage<AuditEntity> result = searchAuditDomainService.searchApiAudit(query, pageable);
        return new Output(result.getContent(), result.getMetadata(), result.getTotalElements());
    }

    public record Input(ApiAuditQueryFilters query, Optional<Pageable> pageable) {
        public Input(ApiAuditQueryFilters query) {
            this(query, Optional.empty());
        }

        public Input(ApiAuditQueryFilters query, Pageable pageable) {
            this(query, Optional.of(pageable));
        }
    }

    public record Output(List<AuditEntity> data, Map<String, String> metadata, long total) {}
}

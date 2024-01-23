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
package io.gravitee.apim.infra.query_service.audit;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.apim.infra.adapter.AuditAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AuditQueryServiceImpl implements AuditQueryService {

    private final AuditRepository auditRepository;

    public AuditQueryServiceImpl(@Lazy AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public SearchResponse searchApiAudit(ApiAuditQueryFilters query, Pageable pageable) {
        AuditCriteria.Builder criteria = new AuditCriteria.Builder()
            .organizationId(query.organizationId())
            .environmentIds(List.of(query.environmentId()))
            .references(Audit.AuditReferenceType.API, List.of(query.apiId()))
            .events(query.events() != null ? new ArrayList<>(query.events()) : null);

        query.from().ifPresent(criteria::from);
        query.to().ifPresent(criteria::to);

        Page<Audit> result = auditRepository.search(
            criteria.build(),
            new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
        );

        return new SearchResponse(result.getTotalElements(), result.getContent().stream().map(AuditAdapter.INSTANCE::toEntity).toList());
    }
}

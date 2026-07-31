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
package inmemory;

import io.gravitee.apim.core.api.model.ApiAuditQueryFilters;
import io.gravitee.apim.core.api_product.model.ApiProductAuditQueryFilters;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.query_service.AuditQueryService;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AuditQueryServiceInMemory implements AuditQueryService, InMemoryAlternative<AuditEntity> {

    private final List<AuditEntity> storage;

    public AuditQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public AuditQueryServiceInMemory(AuditCrudServiceInMemory auditCrudServiceInMemory) {
        this.storage = auditCrudServiceInMemory.storage;
    }

    @Override
    public SearchResponse searchApiAudit(ApiAuditQueryFilters query, Pageable pageable) {
        return search(query.apiId(), query.environmentId(), query.organizationId(), query.events(), query.from(), query.to(), pageable);
    }

    private SearchResponse search(
        String referenceId,
        String environmentId,
        String organizationId,
        java.util.Set<String> events,
        java.util.Optional<Long> from,
        java.util.Optional<Long> to,
        Pageable pageable
    ) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(audit -> audit.getReferenceId().equals(referenceId))
            .filter(audit -> audit.getEnvironmentId().equals(environmentId))
            .filter(audit -> audit.getOrganizationId().equals(organizationId))
            .filter(audit -> events.isEmpty() || events.contains(audit.getEvent()))
            .filter(audit -> from.map(f -> audit.getCreatedAt().toInstant().isAfter(new Date(f).toInstant())).orElse(true))
            .filter(audit -> to.map(t -> audit.getCreatedAt().toInstant().isBefore(new Date(t).toInstant())).orElse(true))
            .sorted(Comparator.comparing(AuditEntity::getCreatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return new SearchResponse(matches.size(), page);
    }

    @Override
    public SearchResponse searchApiProductAudit(ApiProductAuditQueryFilters query, Pageable pageable) {
        return search(
            query.apiProductId(),
            query.environmentId(),
            query.organizationId(),
            query.events(),
            query.from(),
            query.to(),
            pageable
        );
    }

    @Override
    public void initWith(List<AuditEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<AuditEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}

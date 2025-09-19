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
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(audit -> audit.getReferenceId().equals(query.apiId()))
            .filter(audit -> audit.getEnvironmentId().equals(query.environmentId()))
            .filter(audit -> audit.getOrganizationId().equals(query.organizationId()))
            .filter(audit -> query.events().isEmpty() || query.events().contains(audit.getEvent()))
            .filter(audit ->
                query
                    .from()
                    .map(from -> audit.getCreatedAt().toInstant().isAfter(new Date(from).toInstant()))
                    .orElse(true)
            )
            .filter(audit ->
                query
                    .to()
                    .map(to -> audit.getCreatedAt().toInstant().isBefore(new Date(to).toInstant()))
                    .orElse(true)
            )
            .sorted(Comparator.comparing(AuditEntity::getCreatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return new SearchResponse(matches.size(), page);
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

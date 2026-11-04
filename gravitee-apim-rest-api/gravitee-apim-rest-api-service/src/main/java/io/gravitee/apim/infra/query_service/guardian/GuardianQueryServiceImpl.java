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
package io.gravitee.apim.infra.query_service.guardian;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.guardian.model.Guardian;
import io.gravitee.apim.core.guardian.query_service.GuardianQueryService;
import io.gravitee.apim.infra.adapter.GuardianAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GuardianRepository;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Service
public class GuardianQueryServiceImpl implements GuardianQueryService {

    private final GuardianRepository guardianRepository;
    private final GuardianAdapter guardianAdapter;

    public GuardianQueryServiceImpl(@Lazy final GuardianRepository guardianRepository, GuardianAdapter guardianAdapter) {
        this.guardianRepository = guardianRepository;
        this.guardianAdapter = guardianAdapter;
    }

    @Override
    public Page<Guardian> searchByEnvironmentId(String environmentId, String q, Pageable pageable, Sortable sortable) {
        try {
            var criteria = GuardianCriteria.builder().query(q).environmentId(environmentId).build();
            var result = guardianRepository.search(
                criteria,
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build(),
                new SortableBuilder().field(sortable.getField()).setAsc(sortable.isAscOrder()).build()
            );
            return result.map(guardianAdapter::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while searching guardians by environment ID {}", environmentId, e);
            throw new TechnicalDomainException("An error occurred while trying to search guardians by environment ID: " + environmentId, e);
        }
    }
}

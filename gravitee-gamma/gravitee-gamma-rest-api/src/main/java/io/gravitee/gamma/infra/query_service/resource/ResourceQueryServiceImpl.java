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
package io.gravitee.gamma.infra.query_service.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.query_service.ResourceQueryService;
import io.gravitee.gamma.infra.adapter.ResourceAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ResourceRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class ResourceQueryServiceImpl extends AbstractService implements ResourceQueryService {

    private final ResourceRepository resourceRepository;

    public ResourceQueryServiceImpl(@Lazy ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    public Page<Resource> search(Resource.ReferenceType referenceType, String referenceId, Pageable pageable, String query) {
        try {
            var repoPageable = new PageableBuilder()
                .pageNumber(Math.max(0, pageable.getPageNumber() - 1))
                .pageSize(pageable.getPageSize())
                .build();
            return resourceRepository
                .findByReference(ResourceAdapter.INSTANCE.toRepoReferenceType(referenceType), referenceId, repoPageable, query)
                .map(ResourceAdapter.INSTANCE::toCoreModel);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when searching Resources by reference [" + referenceType + "/" + referenceId + "]",
                e
            );
        }
    }
}

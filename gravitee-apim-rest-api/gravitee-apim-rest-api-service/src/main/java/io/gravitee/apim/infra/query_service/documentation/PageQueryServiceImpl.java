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
package io.gravitee.apim.infra.query_service.documentation;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.PageReferenceType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PageQueryServiceImpl implements PageQueryService {

    private final PageRepository pageRepository;
    private static final Logger logger = LoggerFactory.getLogger(PageQueryServiceImpl.class);

    public PageQueryServiceImpl(@Lazy final PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public List<Page> searchByApiId(String apiId) {
        return this.search(new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId(apiId), apiId);
    }

    @Override
    public Optional<Page> findHomepageByApiId(String apiId) {
        PageCriteria criteria = new PageCriteria.Builder()
            .referenceType(PageReferenceType.API.name())
            .referenceId(apiId)
            .homepage(true)
            .build();
        try {
            return pageRepository.search(criteria).stream().findFirst().map(PageAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding all homepage pages with apiId {}", apiId, e);
            throw new TechnicalDomainException("Error during repository search", e);
        }
    }

    /**
     * Get a list of Pages for a specific Api and a parent.
     * If parentId is null, then the parent is assumed to be the root.
     *
     * @param apiId
     * @param parentId
     * @return
     */
    @Override
    public List<Page> searchByApiIdAndParentId(String apiId, String parentId) {
        PageCriteria.Builder pageCriteriaBuilder = new PageCriteria.Builder()
            .referenceType(PageReferenceType.API.name())
            .referenceId(apiId);

        if (Objects.isNull(parentId)) {
            pageCriteriaBuilder.rootParent(true);
        } else {
            pageCriteriaBuilder.parent(parentId);
        }

        return this.search(pageCriteriaBuilder, apiId);
    }

    @Override
    public Optional<Page> findByApiIdAndParentIdAndNameAndType(String apiId, String parentId, String name, Page.Type type) {
        PageCriteria.Builder pageCriteriaBuilder = new PageCriteria.Builder()
            .referenceType(PageReferenceType.API.name())
            .referenceId(apiId)
            .name(name)
            .type(type.name());

        if (Objects.isNull(parentId)) {
            pageCriteriaBuilder.rootParent(true);
        } else {
            pageCriteriaBuilder.parent(parentId);
        }
        return this.search(pageCriteriaBuilder, apiId).stream().findFirst();
    }

    @Override
    public long countByParentIdAndIsPublished(String parentId) {
        try {
            return this.pageRepository.countByParentIdAndIsPublished(parentId);
        } catch (TechnicalException e) {
            logger.error("An error occurred while counting Pages by parentId {}", parentId, e);
            throw new TechnicalDomainException("Error during repository search", e);
        }
    }

    @Override
    public Optional<Page> findByNameAndReferenceId(String name, String referenceId) {
        List<io.gravitee.repository.management.model.Page> result;
        try {
            result = pageRepository.search(new PageCriteria.Builder().name(name).referenceId(referenceId).build());
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Page by name {}", name, e);
            throw new TechnicalDomainException("Error when updating Page", e);
        }
        return switch (result.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(PageAdapter.INSTANCE.toEntity(result.get(0)));
            default -> throw new IllegalStateException("Found more than one page with name " + name);
        };
    }

    private List<Page> search(PageCriteria.Builder pageCriteriaBuilder, String apiId) {
        try {
            return PageAdapter.INSTANCE.toEntityList(pageRepository.search(pageCriteriaBuilder.build()));
        } catch (TechnicalException e) {
            logger.error("An error occurred while searching for Page by apiId {}", apiId, e);
            throw new TechnicalDomainException("Error during repository search", e);
        }
    }
}

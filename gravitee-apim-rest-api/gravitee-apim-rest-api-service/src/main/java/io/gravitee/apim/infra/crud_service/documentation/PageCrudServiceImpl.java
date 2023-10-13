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
package io.gravitee.apim.infra.crud_service.documentation;

import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.*;
import io.gravitee.apim.core.exception.DomainException;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PageCrudServiceImpl implements PageCrudService {

    private final PageRepository pageRepository;
    private static final Logger logger = LoggerFactory.getLogger(PageCrudServiceImpl.class);

    public PageCrudServiceImpl(@Lazy PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public Page createDocumentationPage(Page page) {
        var createdDocumentation = this.createDocumentation(PageAdapter.INSTANCE.toRepository(page));
        return PageAdapter.INSTANCE.toEntity(createdDocumentation);
    }

    @Override
    public Page updateDocumentationPage(Page pageToUpdate) {
        try {
            var updatedPage = pageRepository.update(PageAdapter.INSTANCE.toRepository(pageToUpdate));
            return PageAdapter.INSTANCE.toEntity(updatedPage);
        } catch (TechnicalException e) {
            logger.error("An error occurred while updating homepage attribute from {}", pageToUpdate, e);
            throw new DomainException("Error when updating Page", e);
        }
    }

    @Override
    public Page get(String id) {
        try {
            var foundPage = pageRepository.findById(id);
            if (foundPage.isPresent()) {
                return PageAdapter.INSTANCE.toEntity(foundPage.get());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Page by id {}", id, e);
        }
        throw new PageNotFoundException(id);
    }

    private io.gravitee.repository.management.model.Page createDocumentation(io.gravitee.repository.management.model.Page page) {
        try {
            return pageRepository.create(page);
        } catch (TechnicalException e) {
            logger.error("An error occurred while creating {}", page, e);
            throw new DomainException("Error when creating Page", e);
        }
    }
}

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
import io.gravitee.apim.core.documentation.exception.ApiPageInvalidReferenceTypeException;
import io.gravitee.apim.core.documentation.exception.ApiPageNotDeletedException;
import io.gravitee.apim.core.documentation.model.*;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.Collection;
import java.util.Optional;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PageCrudServiceImpl implements PageCrudService {

    private final PageRepository pageRepository;

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
            throw new TechnicalDomainException(
                String.format("An error occurred while updating page %s.", pageToUpdate),
                e
            );
        }
    }

    @Override
    public Page get(String id) {
        return this.findById(id).orElseThrow(() -> new PageNotFoundException(id));
    }

    @Override
    public Optional<Page> findById(String id) {
        try {
            return pageRepository.findById(id).map(PageAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            log.error("An error occurred while trying to find Page by id {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String id) {
        try {
            pageRepository.delete(id);
        } catch (TechnicalException e) {
            throw new ApiPageNotDeletedException(String.format("An error occurred while trying to delete Page by id %s", id), e);
        }
    }

    @Override
    public void unsetHomepage(Collection<String> ids) {
        try {
            pageRepository.unsetHomepage(ids);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(String.format("An error occurred while trying to unset homepage for Page ids %s", ids), e);
        }
    }

    private io.gravitee.repository.management.model.Page createDocumentation(io.gravitee.repository.management.model.Page page) {
        try {
            return pageRepository.create(page);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while creating Page %s with id %s", page.getName(), page.getId()),
                e
            );
        }
    }
}

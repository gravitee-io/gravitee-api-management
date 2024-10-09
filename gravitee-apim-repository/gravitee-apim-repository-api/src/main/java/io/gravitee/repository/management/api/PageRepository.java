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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PageRepository extends FindAllRepository<Page> {
    Page create(Page page) throws TechnicalException;

    Page update(Page page) throws TechnicalException;

    void delete(String id) throws TechnicalException;

    void unsetHomepage(Collection<String> ids) throws TechnicalException;

    Optional<Page> findById(String id) throws TechnicalException;

    List<Page> search(PageCriteria criteria) throws TechnicalException;

    Integer findMaxPageReferenceIdAndReferenceTypeOrder(String referenceId, PageReferenceType referenceType) throws TechnicalException;

    io.gravitee.common.data.domain.Page<Page> findAll(Pageable pageable) throws TechnicalException;

    long countByParentIdAndIsPublished(String parentId) throws TechnicalException;

    /**
     * Delete pages by reference
     *
     * @param referenceId   Page reference id
     * @param referenceType Page reference Type
     * @return List of IDs for deleted pages
     * @throws TechnicalException
     */
    Map<String, List<String>> deleteByReferenceIdAndReferenceType(String referenceId, PageReferenceType referenceType)
        throws TechnicalException;
}

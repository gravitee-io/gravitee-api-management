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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PageRevision;
import java.util.List;
import java.util.Optional;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PageRevisionRepository extends FindAllRepository<PageRevision> {
    Page<PageRevision> findAll(Pageable pageable) throws TechnicalException;

    Optional<PageRevision> findById(String pageId, int revision) throws TechnicalException;

    PageRevision create(PageRevision item) throws TechnicalException;

    /**
     * List all revision for a given pageId
     * @param pageId
     * @return
     */
    List<PageRevision> findAllByPageId(String pageId) throws TechnicalException;

    /**
     * get the most recent revision for a given pageId
     * @param pageId
     * @return
     */
    Optional<PageRevision> findLastByPageId(String pageId) throws TechnicalException;

<<<<<<< HEAD
    List<String> deleteByPageId(String pageId) throws TechnicalException;
=======
    void deleteAllByPageId(String pageId) throws TechnicalException;
>>>>>>> 9fa0033e0f (fix: delete page revisions when api is deleted)
}

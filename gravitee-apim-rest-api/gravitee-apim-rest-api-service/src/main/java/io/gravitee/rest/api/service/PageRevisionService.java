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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.model.PageRevisionEntity;
import java.util.List;
import java.util.Optional;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PageRevisionService {
    io.gravitee.common.data.domain.Page<PageRevisionEntity> findAll(Pageable pageable);

    Optional<PageRevisionEntity> findById(String pageId, int revision);

    Optional<PageRevisionEntity> findLastByPageId(String pageId);

    List<PageRevisionEntity> findAllByPageId(String pageId);

    PageRevisionEntity create(Page page);

    void deleteAllByPageId(String pageId) throws TechnicalException;
}

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
import io.gravitee.repository.management.api.search.ThemeCriteria;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeReferenceType;
import io.gravitee.repository.management.model.ThemeType;
import java.util.List;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ThemeRepository extends CrudRepository<Theme, String> {
    Set<Theme> findByReferenceIdAndReferenceTypeAndType(String referenceId, String referenceType, ThemeType type) throws TechnicalException;
    Page<Theme> search(ThemeCriteria criteria, Pageable pageable) throws TechnicalException;

    /**
     * Delete themes by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted themes
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, ThemeReferenceType referenceType) throws TechnicalException;
}

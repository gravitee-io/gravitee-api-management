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
import io.gravitee.repository.management.model.Resource;
import java.util.Optional;

public interface ResourceRepository {
    Resource create(Resource resource) throws TechnicalException;

    Resource update(Resource resource) throws TechnicalException;

    Optional<Resource> findById(String id) throws TechnicalException;

    void delete(String id) throws TechnicalException;

    Page<Resource> findByReference(Resource.ReferenceType referenceType, String referenceId, Pageable pageable, String query)
        throws TechnicalException;

    boolean existsByNameAndReference(String name, Resource.ReferenceType referenceType, String referenceId) throws TechnicalException;
}

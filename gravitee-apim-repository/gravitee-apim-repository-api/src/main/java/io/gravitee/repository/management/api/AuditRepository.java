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
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AuditRepository extends CrudRepository<Audit, String> {
    Page<Audit> search(AuditCriteria filter, Pageable pageable);

    default Audit update(Audit item) throws TechnicalException {
        throw new UnsupportedOperationException("Update an audit record is forbidden");
    }

    default void delete(String id) throws TechnicalException {
        throw new UnsupportedOperationException("Delete an audit record is forbidden");
    }

    /**
     * Delete audit by reference
     *
     * @param referenceId   The reference ID
     * @param referenceType The reference type
     * @return List of IDs for deleted audits
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, Audit.AuditReferenceType referenceType) throws TechnicalException;
}

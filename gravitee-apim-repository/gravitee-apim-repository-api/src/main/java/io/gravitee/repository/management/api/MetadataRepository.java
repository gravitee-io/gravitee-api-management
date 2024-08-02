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
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import java.util.List;
import java.util.Optional;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MetadataRepository extends FindAllRepository<Metadata> {
    Metadata create(Metadata metadata) throws TechnicalException;

    Metadata update(Metadata metadata) throws TechnicalException;

    void delete(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException;

    Optional<Metadata> findById(String key, String referenceId, MetadataReferenceType referenceType) throws TechnicalException;

    List<Metadata> findByKeyAndReferenceType(String key, MetadataReferenceType referenceType) throws TechnicalException;

    List<Metadata> findByReferenceType(MetadataReferenceType referenceType) throws TechnicalException;

    List<Metadata> findByReferenceTypeAndReferenceId(MetadataReferenceType referenceType, String referenceId) throws TechnicalException;

    /**
     * Delete Metadata by reference
     *
     * @param referenceId   the ref id
     * @param referenceType The ref type
     * @return List of deleted IDs for metadata
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, MetadataReferenceType referenceType) throws TechnicalException;
}

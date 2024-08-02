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
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import java.util.List;
import java.util.Optional;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CustomUserFieldsRepository extends FindAllRepository<CustomUserField> {
    CustomUserField create(CustomUserField field) throws TechnicalException;

    CustomUserField update(CustomUserField field) throws TechnicalException;

    void delete(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException;

    Optional<CustomUserField> findById(String key, String refId, CustomUserFieldReferenceType refType) throws TechnicalException;

    List<CustomUserField> findByReferenceIdAndReferenceType(String refId, CustomUserFieldReferenceType refType) throws TechnicalException;

    /**
     * Delete custom user fields by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted custom user fields
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, CustomUserFieldReferenceType referenceType)
        throws TechnicalException;
}

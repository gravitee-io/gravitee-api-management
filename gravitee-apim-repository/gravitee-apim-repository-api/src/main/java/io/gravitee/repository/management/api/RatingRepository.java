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
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RatingRepository extends FindAllRepository<Rating> {
    Rating create(Rating rating) throws TechnicalException;

    Optional<Rating> findById(String id) throws TechnicalException;

    Page<Rating> findByReferenceIdAndReferenceTypePageable(String referenceId, RatingReferenceType referenceType, Pageable pageable)
        throws TechnicalException;

    List<Rating> findByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType) throws TechnicalException;

    Rating update(Rating rating) throws TechnicalException;

    void delete(String id) throws TechnicalException;

    Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(String referenceId, RatingReferenceType referenceType, String user)
        throws TechnicalException;

    Set<String> findReferenceIdsOrderByRate(RatingCriteria ratingCriteria) throws TechnicalException;

    /**
     * Delete by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted ratings
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType) throws TechnicalException;
}

/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.bridge.client.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.RatingCriteria;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpRatingRepository extends AbstractRepository implements RatingRepository {

    @Override
    public Rating create(Rating rating) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Rating> findById(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Rating update(Rating rating) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String id) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Page<Rating> findByReferenceIdAndReferenceTypePageable(
        String referenceId,
        RatingReferenceType referenceType,
        Pageable pageable
    ) {
        throw new IllegalStateException();
    }

    @Override
    public List<Rating> findByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(String referenceId, RatingReferenceType referenceType, String user) {
        throw new IllegalStateException();
    }

    @Override
    public Set<String> findReferenceIdsOrderByRate(RatingCriteria ratingCriteria) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Rating> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }
}

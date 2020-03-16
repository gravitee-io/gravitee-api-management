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
package io.gravitee.rest.api.repository.proxy;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Rating;
import io.gravitee.repository.management.model.RatingReferenceType;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RatingRepositoryProxy extends AbstractProxy<RatingRepository> implements RatingRepository {

    @Override
    public Rating create(Rating rating) throws TechnicalException {
        return target.create(rating);
    }

    @Override
    public Optional<Rating> findById(String id) throws TechnicalException {
        return target.findById(id);
    }

    @Override
    public Rating update(Rating rating) throws TechnicalException {
        return target.update(rating);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        target.delete(id);
    }

    @Override
    public Page<Rating> findByReferenceIdAndReferenceTypePageable(String referenceId, RatingReferenceType referenceType,
            Pageable pageable) throws TechnicalException {
        return target.findByReferenceIdAndReferenceTypePageable(referenceId, referenceType, pageable);
    }

    @Override
    public List<Rating> findByReferenceIdAndReferenceType(String referenceId, RatingReferenceType referenceType)
            throws TechnicalException {
        return target.findByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public Optional<Rating> findByReferenceIdAndReferenceTypeAndUser(String referenceId,
            RatingReferenceType referenceType, String user) throws TechnicalException {
        return target.findByReferenceIdAndReferenceTypeAndUser(referenceId, referenceType, user);
    }

}
